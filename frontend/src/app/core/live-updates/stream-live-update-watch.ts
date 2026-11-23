import { signal } from '@angular/core';
import { Observable, Subject, auditTime, merge } from 'rxjs';

import { FORBIDDEN_STATUS, UNAUTHORIZED_STATUS } from '../api-failure';
import { LiveStream } from './live-stream';
import { LiveUpdateWatch } from './live-update-watch';
import { reconnectDelayInMillisecondsFor } from './reconnect-delay';
import { SessionExpiryHandler } from './session-expiry-handler';
import { ServerEvent } from './server-event-parser';
import { StreamRejectedError, StreamRequest } from './stream-request';

const REFRESH_MERGE_WINDOW_IN_MILLISECONDS = 300;
const FALLBACK_POLL_INTERVAL_IN_MILLISECONDS = 5000;

const READY_EVENT_NAME = 'ready';
const CHANGE_EVENT_NAMES: readonly string[] = ['ticket-changed', 'configuration-changed'];

export class StreamLiveUpdateWatch implements LiveUpdateWatch {
  private readonly connected = signal(false);
  private readonly changeSignals = new Subject<void>();
  private readonly fallbackPollTicks = new Subject<void>();

  readonly isConnected = this.connected.asReadonly();
  readonly refreshRequests: Observable<void> = merge(
    this.changeSignals.pipe(auditTime(REFRESH_MERGE_WINDOW_IN_MILLISECONDS)),
    this.fallbackPollTicks,
  );

  private hasStopped = false;
  private hasGivenUpStreaming = false;
  private failedAttemptCount = 0;
  private connectionAbortController: AbortController | null = null;
  private reconnectTimerId: ReturnType<typeof setTimeout> | null = null;
  private fallbackPollTimerId: ReturnType<typeof setInterval> | null = null;

  constructor(
    private readonly liveStream: LiveStream,
    private readonly streamRequest: StreamRequest,
    private readonly sessionExpiryHandler: SessionExpiryHandler,
    private isPageVisible: boolean,
  ) {}

  start(): void {
    this.openStreamWhenAllowed();
    this.updateFallbackPolling();
  }

  pageVisibilityChanged(isPageVisible: boolean): void {
    if (this.hasStopped || isPageVisible === this.isPageVisible) {
      return;
    }
    this.isPageVisible = isPageVisible;
    if (isPageVisible) {
      this.changeSignals.next();
      this.openStreamWhenAllowed();
    } else {
      this.failedAttemptCount = 0;
      this.cancelReconnect();
      this.closeConnection();
    }
    this.updateFallbackPolling();
  }

  stop(): void {
    if (this.hasStopped) {
      return;
    }
    this.hasStopped = true;
    this.cancelReconnect();
    this.closeConnection();
    this.updateFallbackPolling();
    this.changeSignals.complete();
    this.fallbackPollTicks.complete();
  }

  private openStreamWhenAllowed(): void {
    if (this.hasStopped || this.hasGivenUpStreaming || !this.isPageVisible) {
      return;
    }
    this.closeConnection();
    const abortController = new AbortController();
    this.connectionAbortController = abortController;
    void this.readStream(abortController);
  }

  private async readStream(abortController: AbortController): Promise<void> {
    let hasReceivedReady = false;
    try {
      const events = await this.streamRequest.openStream(this.liveStream, abortController.signal);
      for await (const event of events) {
        hasReceivedReady = hasReceivedReady || event.name === READY_EVENT_NAME;
        this.handleEvent(event);
      }
    } catch (error: unknown) {
      if (!abortController.signal.aborted) {
        this.handleConnectionFailure(error);
      }
      return;
    }
    if (!abortController.signal.aborted) {
      this.handleCleanClose(hasReceivedReady);
    }
  }

  private handleEvent(event: ServerEvent): void {
    if (event.name === READY_EVENT_NAME) {
      this.failedAttemptCount = 0;
      this.setConnected(true);
      this.changeSignals.next();
    } else if (CHANGE_EVENT_NAMES.includes(event.name)) {
      this.changeSignals.next();
    }
  }

  private handleCleanClose(hasReceivedReady: boolean): void {
    this.setConnected(false);
    if (!hasReceivedReady) {
      this.failedAttemptCount++;
    }
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
      this.failedAttemptCount++;
      this.scheduleReconnect();
    }
  }

  private scheduleReconnect(): void {
    this.cancelReconnect();
    this.reconnectTimerId = setTimeout(() => {
      this.reconnectTimerId = null;
      this.openStreamWhenAllowed();
    }, reconnectDelayInMillisecondsFor(this.failedAttemptCount));
  }

  private cancelReconnect(): void {
    if (this.reconnectTimerId !== null) {
      clearTimeout(this.reconnectTimerId);
      this.reconnectTimerId = null;
    }
  }

  private closeConnection(): void {
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
