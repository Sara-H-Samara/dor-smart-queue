import { DestroyRef, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { LiveStream } from './live-stream';
import { LiveUpdateService, RANDOM_FRACTION } from './live-update.service';
import { LiveUpdateWatch } from './live-update-watch';
import { PageVisibility } from './page-visibility';
import { ServerEvent } from './server-event-parser';
import { SessionExpiryHandler } from './session-expiry-handler';
import { StreamRejectedError, StreamRequest } from './stream-request';

type StreamItem =
  | { kind: 'event'; event: ServerEvent }
  | { kind: 'serverClosed' }
  | { kind: 'failure'; error: unknown }
  | { kind: 'aborted' };

class FakeOpenedStream {
  private readonly queuedItems: StreamItem[] = [];
  private wakeUpReader: (() => void) | null = null;

  constructor(
    readonly liveStream: LiveStream,
    readonly abortSignal: AbortSignal,
  ) {
    abortSignal.addEventListener('abort', () => this.enqueue({ kind: 'aborted' }));
  }

  sendEvent(name: string): void {
    this.enqueue({ kind: 'event', event: { name, data: '{}' } });
  }

  sendComment(): void {
    this.sendEvent('comment');
  }

  closeFromServer(): void {
    this.enqueue({ kind: 'serverClosed' });
  }

  failWith(error: unknown): void {
    this.enqueue({ kind: 'failure', error });
  }

  async *events(): AsyncGenerator<ServerEvent> {
    for (;;) {
      const item = await this.nextItem();
      if (item.kind === 'serverClosed') {
        return;
      }
      if (item.kind === 'failure') {
        throw item.error;
      }
      if (item.kind === 'aborted') {
        throw new DOMException('The operation was aborted.', 'AbortError');
      }
      yield item.event;
    }
  }

  private enqueue(item: StreamItem): void {
    this.queuedItems.push(item);
    this.wakeUpReader?.();
  }

  private async nextItem(): Promise<StreamItem> {
    while (this.queuedItems.length === 0) {
      await new Promise<void>((resolve) => (this.wakeUpReader = resolve));
    }
    this.wakeUpReader = null;
    return this.queuedItems.shift()!;
  }
}

class FakeLiveServer {
  readonly openedStreams: FakeOpenedStream[] = [];
  private readonly failuresForNextOpenings: unknown[] = [];
  private openingsLeftPending = 0;

  readonly openStream = vi.fn(
    async (
      liveStream: LiveStream,
      abortSignal: AbortSignal,
    ): Promise<AsyncIterable<ServerEvent>> => {
      if (this.openingsLeftPending > 0) {
        this.openingsLeftPending--;
        return new Promise((_resolve, reject) =>
          abortSignal.addEventListener('abort', () =>
            reject(new DOMException('The operation was aborted.', 'AbortError')),
          ),
        );
      }
      if (this.failuresForNextOpenings.length > 0) {
        throw this.failuresForNextOpenings.shift();
      }
      const openedStream = new FakeOpenedStream(liveStream, abortSignal);
      this.openedStreams.push(openedStream);
      return openedStream.events();
    },
  );

  failNextOpenings(count: number, error: unknown): void {
    for (let failure = 0; failure < count; failure++) {
      this.failuresForNextOpenings.push(error);
    }
  }

  leaveNextOpeningPending(): void {
    this.openingsLeftPending++;
  }
}

const networkFailure = new TypeError('Failed to fetch');

describe('LiveUpdateService', () => {
  let server: FakeLiveServer;
  let endSessionAndShowLogin: ReturnType<typeof vi.fn>;
  let isPageVisible: ReturnType<typeof signal<boolean>>;
  let destroyCallbacks: (() => void)[];
  let watch: LiveUpdateWatch;
  let refreshCount: number;
  let refreshRequestsCompleted: boolean;
  let randomFraction: number;

  async function closeOpenedStreamRightAfterReady(streamIndex: number): Promise<void> {
    server.openedStreams[streamIndex].sendEvent('ready');
    server.openedStreams[streamIndex].closeFromServer();
    await advanceTime(0);
  }

  async function advanceTime(milliseconds: number): Promise<void> {
    await vi.advanceTimersByTimeAsync(milliseconds);
  }

  async function startWatching(liveStream: LiveStream = 'student'): Promise<void> {
    const destroyRef = {
      onDestroy: (callback: () => void) => {
        destroyCallbacks.push(callback);
        return () => undefined;
      },
    } as DestroyRef;
    watch = TestBed.inject(LiveUpdateService).watch(liveStream, destroyRef);
    watch.refreshRequests.subscribe({
      next: () => refreshCount++,
      complete: () => (refreshRequestsCompleted = true),
    });
    TestBed.tick();
    await advanceTime(0);
  }

  async function connectAndReceiveReady(): Promise<FakeOpenedStream> {
    await startWatching();
    const openedStream = server.openedStreams[0];
    openedStream.sendEvent('ready');
    await advanceTime(300);
    return openedStream;
  }

  async function changePageVisibility(visible: boolean): Promise<void> {
    isPageVisible.set(visible);
    TestBed.tick();
    await advanceTime(0);
  }

  function destroyScreen(): void {
    destroyCallbacks.forEach((callback) => callback());
  }

  beforeEach(() => {
    vi.useFakeTimers();
    server = new FakeLiveServer();
    endSessionAndShowLogin = vi.fn();
    isPageVisible = signal(true);
    destroyCallbacks = [];
    refreshCount = 0;
    refreshRequestsCompleted = false;
    randomFraction = 0;
    TestBed.configureTestingModule({
      providers: [
        { provide: RANDOM_FRACTION, useValue: () => randomFraction },
        { provide: StreamRequest, useValue: { openStream: server.openStream } },
        { provide: SessionExpiryHandler, useValue: { endSessionAndShowLogin } },
        { provide: PageVisibility, useValue: { isVisible: isPageVisible } },
      ],
    });
  });

  afterEach(() => {
    destroyScreen();
    vi.useRealTimers();
  });

  describe('opening the stream', () => {
    it('opens the stream that the screen asked for', async () => {
      await startWatching('staff');

      expect(server.openStream).toHaveBeenCalledTimes(1);
      expect(server.openedStreams[0].liveStream).toBe('staff');
    });

    it('is not connected until the server says ready', async () => {
      await startWatching();

      expect(watch.isConnected()).toBe(false);
    });

    it('is connected after the server says ready', async () => {
      await connectAndReceiveReady();

      expect(watch.isConnected()).toBe(true);
    });
  });

  describe('asking the screen to refresh', () => {
    it('asks for a refresh after ready', async () => {
      await connectAndReceiveReady();

      expect(refreshCount).toBe(1);
    });

    it('asks for a refresh after a ticket-changed event', async () => {
      const openedStream = await connectAndReceiveReady();

      openedStream.sendEvent('ticket-changed');
      await advanceTime(300);

      expect(refreshCount).toBe(2);
    });

    it('asks for a refresh after a configuration-changed event', async () => {
      const openedStream = await connectAndReceiveReady();

      openedStream.sendEvent('configuration-changed');
      await advanceTime(300);

      expect(refreshCount).toBe(2);
    });

    it('ignores an event with an unknown name', async () => {
      const openedStream = await connectAndReceiveReady();

      openedStream.sendEvent('something-else');
      await advanceTime(1000);

      expect(refreshCount).toBe(1);
    });

    it('asks for only one refresh when many events arrive close together', async () => {
      await startWatching();
      const openedStream = server.openedStreams[0];

      openedStream.sendEvent('ready');
      openedStream.sendEvent('ticket-changed');
      await advanceTime(100);
      openedStream.sendEvent('ticket-changed');
      openedStream.sendEvent('configuration-changed');
      await advanceTime(100);
      openedStream.sendEvent('ticket-changed');
      await advanceTime(1000);

      expect(refreshCount).toBe(1);
    });

    it('asks for a second refresh when an event arrives after the first merge window', async () => {
      const openedStream = await connectAndReceiveReady();

      openedStream.sendEvent('ticket-changed');
      await advanceTime(300);
      openedStream.sendEvent('ticket-changed');
      await advanceTime(300);

      expect(refreshCount).toBe(3);
    });
  });

  describe('reconnecting', () => {
    it('opens a new stream one second after the server closes the stream', async () => {
      const openedStream = await connectAndReceiveReady();

      openedStream.closeFromServer();
      await advanceTime(999);
      expect(server.openStream).toHaveBeenCalledTimes(1);
      await advanceTime(1);

      expect(server.openStream).toHaveBeenCalledTimes(2);
    });

    it('is not connected while it waits to reconnect', async () => {
      const openedStream = await connectAndReceiveReady();

      openedStream.closeFromServer();
      await advanceTime(0);

      expect(watch.isConnected()).toBe(false);
    });

    it('asks for a refresh after it reconnects and the server says ready again', async () => {
      const openedStream = await connectAndReceiveReady();
      openedStream.closeFromServer();
      await advanceTime(1000);

      server.openedStreams[1].sendEvent('ready');
      await advanceTime(300);

      expect(refreshCount).toBe(2);
    });

    it('opens a new stream one second after a read error', async () => {
      const openedStream = await connectAndReceiveReady();

      openedStream.failWith(networkFailure);
      await advanceTime(1000);

      expect(server.openStream).toHaveBeenCalledTimes(2);
    });

    it('opens a new stream after the server answers with an error status', async () => {
      server.failNextOpenings(1, new StreamRejectedError(503));
      await startWatching();

      await advanceTime(1000);

      expect(server.openStream).toHaveBeenCalledTimes(2);
    });

    it('waits longer after each failed attempt', async () => {
      server.failNextOpenings(3, networkFailure);
      await startWatching();
      expect(server.openStream).toHaveBeenCalledTimes(1);

      await advanceTime(1000);
      expect(server.openStream).toHaveBeenCalledTimes(2);

      await advanceTime(1999);
      expect(server.openStream).toHaveBeenCalledTimes(2);
      await advanceTime(1);
      expect(server.openStream).toHaveBeenCalledTimes(3);

      await advanceTime(3999);
      expect(server.openStream).toHaveBeenCalledTimes(3);
      await advanceTime(1);
      expect(server.openStream).toHaveBeenCalledTimes(4);
    });

    it('reconnecting_afterReadyThenCloseRepeatedly_waitsOneThenTwoThenFourSeconds', async () => {
      await startWatching();

      await closeOpenedStreamRightAfterReady(0);
      await advanceTime(999);
      expect(server.openStream).toHaveBeenCalledTimes(1);
      await advanceTime(1);
      expect(server.openStream).toHaveBeenCalledTimes(2);

      await closeOpenedStreamRightAfterReady(1);
      await advanceTime(1999);
      expect(server.openStream).toHaveBeenCalledTimes(2);
      await advanceTime(1);
      expect(server.openStream).toHaveBeenCalledTimes(3);

      await closeOpenedStreamRightAfterReady(2);
      await advanceTime(3999);
      expect(server.openStream).toHaveBeenCalledTimes(3);
      await advanceTime(1);
      expect(server.openStream).toHaveBeenCalledTimes(4);
    });

    it('reconnecting_afterStreamStayedOpenThirtySeconds_waitsOneSecond', async () => {
      await startWatching();
      await closeOpenedStreamRightAfterReady(0);
      await advanceTime(1000);
      await closeOpenedStreamRightAfterReady(1);
      await advanceTime(2000);
      expect(server.openStream).toHaveBeenCalledTimes(3);
      server.openedStreams[2].sendEvent('ready');
      await advanceTime(0);
      await advanceTime(30000);

      server.openedStreams[2].closeFromServer();
      await advanceTime(999);
      expect(server.openStream).toHaveBeenCalledTimes(3);
      await advanceTime(1);

      expect(server.openStream).toHaveBeenCalledTimes(4);
    });

    it('reconnecting_afterStreamStayedOpenTwentyNineSeconds_keepsTheLongerWait', async () => {
      await startWatching();
      await closeOpenedStreamRightAfterReady(0);
      await advanceTime(1000);
      await closeOpenedStreamRightAfterReady(1);
      await advanceTime(2000);
      expect(server.openStream).toHaveBeenCalledTimes(3);
      server.openedStreams[2].sendEvent('ready');
      await advanceTime(0);
      await advanceTime(29999);

      server.openedStreams[2].closeFromServer();
      await advanceTime(3999);
      expect(server.openStream).toHaveBeenCalledTimes(3);
      await advanceTime(1);

      expect(server.openStream).toHaveBeenCalledTimes(4);
    });

    it('reconnecting_whenStreamClosesBeforeThirtySeconds_oldTimerDoesNotResetTheCounter', async () => {
      server.failNextOpenings(5, networkFailure);
      await startWatching();
      await advanceTime(31000);
      expect(server.openStream).toHaveBeenCalledTimes(6);
      server.openedStreams[0].sendEvent('ready');
      await advanceTime(2000);
      server.openedStreams[0].closeFromServer();
      await advanceTime(30000);
      expect(server.openStream).toHaveBeenCalledTimes(7);

      await closeOpenedStreamRightAfterReady(1);
      await advanceTime(29999);
      expect(server.openStream).toHaveBeenCalledTimes(7);
      await advanceTime(1);

      expect(server.openStream).toHaveBeenCalledTimes(8);
    });

    it('reconnecting_whenStreamFailsBeforeThirtySeconds_oldTimerDoesNotResetTheCounter', async () => {
      server.failNextOpenings(5, networkFailure);
      await startWatching();
      await advanceTime(31000);
      expect(server.openStream).toHaveBeenCalledTimes(6);
      server.openedStreams[0].sendEvent('ready');
      await advanceTime(2000);
      server.openedStreams[0].failWith(networkFailure);
      await advanceTime(30000);
      expect(server.openStream).toHaveBeenCalledTimes(7);

      await closeOpenedStreamRightAfterReady(1);
      await advanceTime(29999);
      expect(server.openStream).toHaveBeenCalledTimes(7);
      await advanceTime(1);

      expect(server.openStream).toHaveBeenCalledTimes(8);
    });

    it('reconnecting_withRandomFractionOneHalf_waitsTenPercentLonger', async () => {
      randomFraction = 0.5;
      await startWatching();

      server.openedStreams[0].closeFromServer();
      await advanceTime(1099);
      expect(server.openStream).toHaveBeenCalledTimes(1);
      await advanceTime(1);

      expect(server.openStream).toHaveBeenCalledTimes(2);
    });

    it('reconnecting_afterServerClosesTwiceInARow_waitsOneThenTwoSeconds', async () => {
      await startWatching();
      server.openedStreams[0].closeFromServer();
      await advanceTime(1000);
      expect(server.openStream).toHaveBeenCalledTimes(2);

      server.openedStreams[1].closeFromServer();
      await advanceTime(1999);
      expect(server.openStream).toHaveBeenCalledTimes(2);
      await advanceTime(1);

      expect(server.openStream).toHaveBeenCalledTimes(3);
    });
  });

  describe('when the server rejects the token', () => {
    beforeEach(() => {
      server.failNextOpenings(1, new StreamRejectedError(401));
    });

    it('logs the user out when the server answers 401', async () => {
      await startWatching();

      expect(endSessionAndShowLogin).toHaveBeenCalledTimes(1);
    });

    it('does not open the stream again after a 401', async () => {
      await startWatching();

      await advanceTime(120000);

      expect(server.openStream).toHaveBeenCalledTimes(1);
    });

    it('does not poll after a 401', async () => {
      await startWatching();

      await advanceTime(120000);

      expect(refreshCount).toBe(0);
    });

    it('ends the refresh requests after a 401', async () => {
      await startWatching();

      expect(refreshRequestsCompleted).toBe(true);
    });

    it('is not connected after a 401', async () => {
      await startWatching();

      expect(watch.isConnected()).toBe(false);
    });
  });

  describe('when the role is wrong', () => {
    beforeEach(() => {
      server.failNextOpenings(1, new StreamRejectedError(403));
    });

    it('does not log the user out when the server answers 403', async () => {
      await startWatching();

      await advanceTime(60000);

      expect(endSessionAndShowLogin).not.toHaveBeenCalled();
    });

    it('does not open the stream again after a 403', async () => {
      await startWatching();

      await advanceTime(60000);

      expect(server.openStream).toHaveBeenCalledTimes(1);
    });

    it('keeps asking for a refresh every five seconds after a 403', async () => {
      await startWatching();

      await advanceTime(15000);

      expect(refreshCount).toBe(3);
    });

    it('does not open the stream again when the tab is shown again after a 403', async () => {
      await startWatching();

      await changePageVisibility(false);
      await changePageVisibility(true);
      await advanceTime(60000);

      expect(server.openStream).toHaveBeenCalledTimes(1);
    });
  });

  describe('polling as a fallback', () => {
    it('asks for a refresh every five seconds while the stream is not connected', async () => {
      server.failNextOpenings(1, networkFailure);
      await startWatching();

      await advanceTime(5000);
      expect(refreshCount).toBe(1);
      await advanceTime(5000);

      expect(refreshCount).toBe(2);
    });

    it('stops asking for refreshes every five seconds once the stream is connected', async () => {
      await startWatching();
      await advanceTime(5000);
      expect(refreshCount).toBe(1);
      server.openedStreams[0].sendEvent('ready');
      await advanceTime(300);
      expect(refreshCount).toBe(2);

      await advanceTime(60000);

      expect(refreshCount).toBe(2);
    });

    it('starts asking for refreshes again when the connection is lost', async () => {
      const openedStream = await connectAndReceiveReady();
      server.failNextOpenings(10, networkFailure);

      openedStream.closeFromServer();
      await advanceTime(5000);

      expect(refreshCount).toBe(2);
    });

    it('does not ask for refreshes every five seconds while the tab is hidden', async () => {
      await startWatching();
      await changePageVisibility(false);

      await advanceTime(60000);

      expect(refreshCount).toBe(0);
    });
  });

  describe('when the tab is hidden', () => {
    it('hide_whileConnected_keepsStreamOpen', async () => {
      const openedStream = await connectAndReceiveReady();

      await changePageVisibility(false);

      expect(openedStream.abortSignal.aborted).toBe(false);
    });

    it('hide_whileStreamIsOpening_doesNotAbortTheOpeningRequest', async () => {
      server.leaveNextOpeningPending();
      await startWatching();
      const abortSignalOfOpening = server.openStream.mock.calls[0][1];

      await changePageVisibility(false);

      expect(abortSignalOfOpening.aborted).toBe(false);
    });

    it('hide_whileWaitingToReconnect_stillReconnectsOnTime', async () => {
      const openedStream = await connectAndReceiveReady();
      openedStream.closeFromServer();
      await advanceTime(500);

      await changePageVisibility(false);
      await advanceTime(500);

      expect(server.openStream).toHaveBeenCalledTimes(2);
    });

    it('hide_thenTicketChanged_doesNotAskForRefresh', async () => {
      const openedStream = await connectAndReceiveReady();
      await changePageVisibility(false);

      openedStream.sendEvent('ticket-changed');
      await advanceTime(1000);

      expect(refreshCount).toBe(1);
    });
  });

  describe('when the tab is shown again', () => {
    it('show_afterManyChangesWhileHidden_asksForExactlyOneRefresh', async () => {
      const openedStream = await connectAndReceiveReady();
      await changePageVisibility(false);
      openedStream.sendEvent('ticket-changed');
      openedStream.sendEvent('configuration-changed');
      openedStream.sendEvent('ticket-changed');
      await advanceTime(1000);
      expect(refreshCount).toBe(1);

      await changePageVisibility(true);
      await advanceTime(1000);

      expect(refreshCount).toBe(2);
    });

    it('show_afterChangeWasAlreadyShown_secondShow_doesNotAskForRefresh', async () => {
      const openedStream = await connectAndReceiveReady();
      await changePageVisibility(false);
      openedStream.sendEvent('ticket-changed');
      await advanceTime(1000);
      await changePageVisibility(true);
      await advanceTime(1000);
      expect(refreshCount).toBe(2);

      await changePageVisibility(false);
      await changePageVisibility(true);
      await advanceTime(1000);

      expect(refreshCount).toBe(2);
    });

    it('show_afterHiddenWithoutChange_whileConnected_doesNotAskForRefresh', async () => {
      await connectAndReceiveReady();
      await changePageVisibility(false);
      await advanceTime(30000);

      await changePageVisibility(true);
      await advanceTime(1000);

      expect(refreshCount).toBe(1);
    });

    it('show_whileDisconnected_asksForOneRefresh', async () => {
      server.failNextOpenings(10, networkFailure);
      await startWatching();
      await changePageVisibility(false);

      await changePageVisibility(true);
      await advanceTime(300);

      expect(refreshCount).toBe(1);
    });

    it('show_whileDisconnected_resumesFiveSecondPolling', async () => {
      server.leaveNextOpeningPending();
      await startWatching();
      await changePageVisibility(false);

      await changePageVisibility(true);
      await advanceTime(5000);

      expect(refreshCount).toBe(2);
    });

    it('show_whileWaitingToReconnect_doesNotSkipTheBackoff', async () => {
      server.failNextOpenings(1, networkFailure);
      await startWatching();
      await changePageVisibility(false);
      await advanceTime(500);

      await changePageVisibility(true);
      expect(server.openStream).toHaveBeenCalledTimes(1);
      await advanceTime(499);
      expect(server.openStream).toHaveBeenCalledTimes(1);
      await advanceTime(1);

      expect(server.openStream).toHaveBeenCalledTimes(2);
    });

    it('show_afterSilenceLongerThanSixtySeconds_abortsOldStreamAndOpensNewOne', async () => {
      const openedStream = await connectAndReceiveReady();
      await changePageVisibility(false);
      await advanceTime(61000);

      await changePageVisibility(true);

      expect(openedStream.abortSignal.aborted).toBe(true);
      expect(server.openStream).toHaveBeenCalledTimes(2);
    });

    it('show_afterSilenceLongerThanSixtySeconds_thenReady_asksForOneRefresh', async () => {
      await connectAndReceiveReady();
      await changePageVisibility(false);
      await advanceTime(61000);

      await changePageVisibility(true);
      server.openedStreams[1].sendEvent('ready');
      await advanceTime(1000);

      expect(refreshCount).toBe(2);
    });

    it('show_afterHeartbeatsKeptArriving_doesNotOpenNewStream', async () => {
      const openedStream = await connectAndReceiveReady();
      await changePageVisibility(false);
      for (let heartbeatCount = 0; heartbeatCount < 10; heartbeatCount++) {
        await advanceTime(25000);
        openedStream.sendComment();
        await advanceTime(0);
      }

      await changePageVisibility(true);
      await advanceTime(1000);

      expect(server.openStream).toHaveBeenCalledTimes(1);
      expect(refreshCount).toBe(1);
    });

    it('start_whenPageIsHidden_thenShown_asksForOneRefresh', async () => {
      isPageVisible.set(false);
      await startWatching();
      server.openedStreams[0].sendEvent('ready');
      await advanceTime(1000);
      expect(refreshCount).toBe(0);

      await changePageVisibility(true);
      await advanceTime(1000);

      expect(refreshCount).toBe(1);
    });

    it('heartbeatComment_whileVisible_doesNotAskForRefresh', async () => {
      const openedStream = await connectAndReceiveReady();

      openedStream.sendComment();
      await advanceTime(1000);

      expect(refreshCount).toBe(1);
    });
  });

  describe('when the screen is destroyed', () => {
    it('stops the stream request with the abort signal', async () => {
      const openedStream = await connectAndReceiveReady();
      expect(openedStream.abortSignal.aborted).toBe(false);

      destroyScreen();

      expect(openedStream.abortSignal.aborted).toBe(true);
    });

    it('is not connected after the screen is destroyed', async () => {
      await connectAndReceiveReady();

      destroyScreen();

      expect(watch.isConnected()).toBe(false);
    });

    it('does not open a new stream after the screen is destroyed', async () => {
      await connectAndReceiveReady();

      destroyScreen();
      await advanceTime(120000);

      expect(server.openStream).toHaveBeenCalledTimes(1);
    });

    it('does not reconnect when the screen is destroyed while the stream waits to reconnect', async () => {
      const openedStream = await connectAndReceiveReady();
      openedStream.closeFromServer();
      await advanceTime(500);

      destroyScreen();
      await advanceTime(120000);

      expect(server.openStream).toHaveBeenCalledTimes(1);
    });

    it('stops the stream request when the screen is destroyed while the stream is still opening', async () => {
      server.leaveNextOpeningPending();
      await startWatching();
      const abortSignalOfOpening = server.openStream.mock.calls[0][1];

      destroyScreen();
      await advanceTime(120000);

      expect(abortSignalOfOpening.aborted).toBe(true);
      expect(server.openStream).toHaveBeenCalledTimes(1);
    });

    it('does not poll after the screen is destroyed', async () => {
      await startWatching();

      destroyScreen();
      await advanceTime(60000);

      expect(refreshCount).toBe(0);
    });

    it('does not ask for a refresh from events that arrive after the screen was destroyed', async () => {
      const openedStream = await connectAndReceiveReady();
      destroyScreen();

      openedStream.sendEvent('ticket-changed');
      await advanceTime(1000);

      expect(refreshCount).toBe(1);
    });

    it('ends the refresh requests', async () => {
      await startWatching();

      destroyScreen();

      expect(refreshRequestsCompleted).toBe(true);
    });

    it('ignores a tab change after the screen is destroyed', async () => {
      await connectAndReceiveReady();
      destroyScreen();

      await changePageVisibility(false);
      await changePageVisibility(true);

      expect(server.openStream).toHaveBeenCalledTimes(1);
    });
  });

  describe('stopping by hand', () => {
    it('stops the stream request with the abort signal', async () => {
      const openedStream = await connectAndReceiveReady();

      watch.stop();

      expect(openedStream.abortSignal.aborted).toBe(true);
    });

    it('can be called twice without a problem', async () => {
      await connectAndReceiveReady();

      watch.stop();

      expect(() => watch.stop()).not.toThrow();
    });
  });
});
