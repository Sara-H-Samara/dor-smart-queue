import { signal } from '@angular/core';
import { Observable, ReplaySubject, Subject, auditTime, merge } from 'rxjs';

import { FORBIDDEN_STATUS, UNAUTHORIZED_STATUS } from '../api-failure';
import { LiveStream } from './live-stream';
import { LiveUpdateWatch } from './live-update-watch';
import { reconnectDelayWithJitterInMillisecondsFor } from './reconnect-delay';
import { SessionExpiryHandler } from './session-expiry-handler';
import { ServerEvent } from './server-event-parser';
import { StreamRejectedError, StreamRequest } from './stream-request';

const REFRESH_MERGE_WINDOW_IN_MILLISECONDS = 300;
const FALLBACK_POLL_INTERVAL_IN_MILLISECONDS = 5000;
const STABLE_CONNECTION_DURATION_IN_MILLISECONDS = 30000;
const SILENT_CONNECTION_LIMIT_IN_MILLISECONDS = 60000;
const FIRST_LOAD_WAIT_FOR_STREAM_IN_MILLISECONDS = 1000;

const READY_EVENT_NAME = 'ready';
const CHANGE_EVENT_NAMES: readonly string[] = ['ticket-changed', 'configuration-changed'];

export class StreamLiveUpdateWatch implements LiveUpdateWatch {
  private readonly connected = signal(false);
  private readonly changeSignals = new Subject<void>();
  private readonly fallbackPollTicks = new Subject<void>();
  private readonly firstLoadSignals = new ReplaySubject<void>(1);

  readonly isConnected = this.connected.asReadonly();
  readonly firstLoadRequest: Observable<void> = this.firstLoadSignals.asObservable();
  readonly refreshRequests: Observable<void> = merge(
    this.changeSignals.pipe(auditTime(REFRESH_MERGE_WINDOW_IN_MILLISECONDS)),
    this.fallbackPollTicks,
  );

  private hasChangeWaitingForVisiblePage = false;
  private isFirstLoadDue = false;
  private isFirstLoadWaitingForVisiblePage = false;
  private firstLoadTimerId: ReturnType<typeof setTimeout> | null = null;
  private lastServerActivityInMilliseconds = Date.now();
  private hasStopped = false;
  private hasGivenUpStreaming = false;
  private failedAttemptCount = 0;
  private connectionAbortController: AbortController | null = null;
  private reconnectTimerId: ReturnType<typeof setTimeout> | null = null;
  private stableConnectionTimerId: ReturnType<typeof setTimeout> | null = null;
  private fallbackPollTimerId: ReturnType<typeof setInterval> | null = null;

  constructor(
    private readonly liveStream: LiveStream,
    private readonly streamRequest: StreamRequest,
    private readonly sessionExpiryHandler: SessionExpiryHandler,
    private isPageVisible: boolean,
    private readonly randomFraction: () => number,
  ) {}

  start(): void {
    this.openStreamWhenAllowed();
    this.updateFallbackPolling();
    this.startFirstLoadTimer();
  }

  private startFirstLoadTimer(): void {
    this.firstLoadTimerId = setTimeout(() => {
      this.firstLoadTimerId = null;
      this.makeFirstLoadDue();
    }, FIRST_LOAD_WAIT_FOR_STREAM_IN_MILLISECONDS);
  }

  private cancelFirstLoadTimer(): void {
    if (this.firstLoadTimerId !== null) {
      clearTimeout(this.firstLoadTimerId);
      this.firstLoadTimerId = null;
    }
  }

  private makeFirstLoadDue(): void {
    if (this.hasStopped) {
      return;
    }
    this.isFirstLoadDue = true;
    this.cancelFirstLoadTimer();
    if (this.isPageVisible) {
      this.requestFirstLoad();
    } else {
      this.isFirstLoadWaitingForVisiblePage = true;
    }
  }

  private requestFirstLoad(): void {
    this.firstLoadSignals.next();
    this.firstLoadSignals.complete();
  }

  pageVisibilityChanged(isPageVisible: boolean): void {
    if (this.hasStopped || isPageVisible === this.isPageVisible) {
      return;
    }
    this.isPageVisible = isPageVisible;
    if (isPageVisible) {
      this.handlePageBecameVisible();
    }
    this.updateFallbackPolling();
  }

  private handlePageBecameVisible(): void {
    if (this.isFirstLoadWaitingForVisiblePage) {
      this.isFirstLoadWaitingForVisiblePage = false;
      this.hasChangeWaitingForVisiblePage = false;
      this.requestFirstLoad();
    } else if (this.isFirstLoadDue && (this.hasChangeWaitingForVisiblePage || !this.connected())) {
      this.hasChangeWaitingForVisiblePage = false;
      this.changeSignals.next();
    }
    if (this.connected() && !this.hasHeardFromServerRecently()) {
      this.replaceSilentConnection();
    }
  }

  private hasHeardFromServerRecently(): boolean {
    return (
      Date.now() - this.lastServerActivityInMilliseconds <= SILENT_CONNECTION_LIMIT_IN_MILLISECONDS
    );
  }

  private replaceSilentConnection(): void {
    this.cancelReconnect();
    this.openStreamWhenAllowed();
  }

  private announceChangeWhenPageIsVisible(): void {
    if (this.isPageVisible) {
      this.changeSignals.next();
    } else {
      this.hasChangeWaitingForVisiblePage = true;
    }
  }

  stop(): void {
    if (this.hasStopped) {
      return;
    }
    this.hasStopped = true;
    this.cancelFirstLoadTimer();
    this.cancelReconnect();
    this.closeConnection();
    this.updateFallbackPolling();
    this.changeSignals.complete();
    this.fallbackPollTicks.complete();
    this.firstLoadSignals.complete();
  }

  private openStreamWhenAllowed(): void {
    if (this.hasStopped || this.hasGivenUpStreaming) {
      return;
    }
    this.closeConnection();
    this.lastServerActivityInMilliseconds = Date.now();
    const abortController = new AbortController();
    this.connectionAbortController = abortController;
    void this.readStream(abortController);
  }

  private async readStream(abortController: AbortController): Promise<void> {
    try {
      const events = await this.streamRequest.openStream(this.liveStream, abortController.signal);
      for await (const event of events) {
        this.handleEvent(event);
      }
    } catch (error: unknown) {
      if (!abortController.signal.aborted) {
        this.handleConnectionFailure(error);
      }
      return;
    }
    if (!abortController.signal.aborted) {
      this.handleCleanClose();
    }
  }

  private handleEvent(event: ServerEvent): void {
    this.lastServerActivityInMilliseconds = Date.now();
    if (event.name === READY_EVENT_NAME) {
      this.startStableConnectionTimer();
      this.setConnected(true);
      this.handleReady();
    } else if (CHANGE_EVENT_NAMES.includes(event.name)) {
      this.announceChangeWhenPageIsVisible();
    }
  }

  private handleReady(): void {
    if (this.isFirstLoadDue) {
      this.announceChangeWhenPageIsVisible();
    } else {
      this.makeFirstLoadDue();
    }
  }

  private handleCleanClose(): void {
    this.setConnected(false);
    this.cancelStableConnectionTimer();
    this.failedAttemptCount++;
    this.scheduleReconnect();
  }

  private handleConnectionFailure(error: unknown): void {
    if (error instanceof StreamRejectedError && error.status === UNAUTHORIZED_STATUS) {
      this.stop();
      this.sessionExpiryHandler.endSessionAndShowLogin();
    } else if (error instanceof StreamRejectedError && error.status === FORBIDDEN_STATUS) {
      this.hasGivenUpStreaming = true;
      this.closeConnection();
    } else {
      this.setConnected(false);
      this.cancelStableConnectionTimer();
      this.failedAttemptCount++;
      this.scheduleReconnect();
    }
  }

  private startStableConnectionTimer(): void {
    this.cancelStableConnectionTimer();
    this.stableConnectionTimerId = setTimeout(() => {
      this.stableConnectionTimerId = null;
      this.failedAttemptCount = 0;
    }, STABLE_CONNECTION_DURATION_IN_MILLISECONDS);
  }

  private cancelStableConnectionTimer(): void {
    if (this.stableConnectionTimerId !== null) {
      clearTimeout(this.stableConnectionTimerId);
      this.stableConnectionTimerId = null;
    }
  }

  private scheduleReconnect(): void {
    this.cancelReconnect();
    const reconnectDelay = reconnectDelayWithJitterInMillisecondsFor(
      this.failedAttemptCount,
      this.randomFraction(),
    );
    this.reconnectTimerId = setTimeout(() => {
      this.reconnectTimerId = null;
      this.openStreamWhenAllowed();
    }, reconnectDelay);
  }

  private cancelReconnect(): void {
    if (this.reconnectTimerId !== null) {
      clearTimeout(this.reconnectTimerId);
      this.reconnectTimerId = null;
    }
  }

  private closeConnection(): void {
    this.cancelStableConnectionTimer();
    this.connectionAbortController?.abort();
    this.connectionAbortController = null;
    this.setConnected(false);
  }

  private setConnected(isConnected: boolean): void {
    this.connected.set(isConnected);
    this.updateFallbackPolling();
  }

  private updateFallbackPolling(): void {
    const shouldPoll = !this.hasStopped && this.isPageVisible && !this.connected();
    if (shouldPoll && this.fallbackPollTimerId === null) {
      this.fallbackPollTimerId = setInterval(
        () => this.fallbackPollTicks.next(),
        FALLBACK_POLL_INTERVAL_IN_MILLISECONDS,
      );
    } else if (!shouldPoll && this.fallbackPollTimerId !== null) {
      clearInterval(this.fallbackPollTimerId);
      this.fallbackPollTimerId = null;
    }
  }
}
