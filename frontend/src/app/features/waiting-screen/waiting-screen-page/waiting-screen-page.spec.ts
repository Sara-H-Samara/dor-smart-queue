import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, of } from 'rxjs';

import { LiveUpdateService } from '../../../core/live-updates/live-update.service';
import { WaitingRoom } from '../../../core/models/waiting-room.model';
import { WaitingRoomService } from '../../../core/services/waiting-room.service';
import { FakeLiveUpdateService } from '../../../testing/fake-live-update-service';
import { failingRequestWithStatus } from '../../../testing/http-failure';
import { WaitingScreenPage } from './waiting-screen-page';

const waitingRoomWithCalledAndIdleCounter: WaitingRoom = {
  counters: [
    { counterName: 'Counter 1', serviceName: 'Registration', ticketCode: 'R-012' },
    { counterName: 'Counter 3', serviceName: 'Documents', ticketCode: null },
  ],
  nextTickets: [
    { ticketCode: 'R-015', serviceName: 'Registration' },
    { ticketCode: 'D-008', serviceName: 'Documents' },
    { ticketCode: 'R-016', serviceName: 'Registration' },
  ],
  moreWaitingCount: 4,
};

describe('WaitingScreenPage', () => {
  let fetchWaitingRoom: ReturnType<typeof vi.fn>;
  let fakeLiveUpdateService: FakeLiveUpdateService;

  beforeEach(() => {
    fetchWaitingRoom = vi.fn();
    fakeLiveUpdateService = new FakeLiveUpdateService();
    TestBed.configureTestingModule({
      providers: [
        { provide: WaitingRoomService, useValue: { fetchWaitingRoom } },
        { provide: LiveUpdateService, useValue: fakeLiveUpdateService },
      ],
    });
  });

  function createPageBeforeFirstLoad(): ComponentFixture<WaitingScreenPage> {
    const fixture = TestBed.createComponent(WaitingScreenPage);
    fixture.detectChanges();
    return fixture;
  }

  function createPage(): ComponentFixture<WaitingScreenPage> {
    const fixture = createPageBeforeFirstLoad();
    fakeLiveUpdateService.liveUpdateWatch.requestFirstLoad();
    fixture.detectChanges();
    return fixture;
  }

  function pageElement(fixture: ComponentFixture<WaitingScreenPage>): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function textsOf(fixture: ComponentFixture<WaitingScreenPage>, selector: string): string[] {
    return Array.from(pageElement(fixture).querySelectorAll(selector)).map(
      (element) => element.textContent?.replace(/\s+/g, ' ').trim() ?? '',
    );
  }

  it('shows one tile for each counter with the counter name and the service name', () => {
    fetchWaitingRoom.mockReturnValue(of(waitingRoomWithCalledAndIdleCounter));

    const fixture = createPage();

    expect(textsOf(fixture, '.counter-name')).toEqual(['Counter 1', 'Counter 3']);
    expect(textsOf(fixture, '.service-name')).toEqual(['Registration', 'Documents']);
  });

  it('shows the called ticket code on its counter and Not serving on the idle counter', () => {
    fetchWaitingRoom.mockReturnValue(of(waitingRoomWithCalledAndIdleCounter));

    const fixture = createPage();

    expect(textsOf(fixture, '.ticket-code')).toEqual(['R-012']);
    expect(textsOf(fixture, '.not-serving')).toEqual(['Not serving']);
  });

  it('shows the next tickets with their service names', () => {
    fetchWaitingRoom.mockReturnValue(of(waitingRoomWithCalledAndIdleCounter));

    const fixture = createPage();

    expect(textsOf(fixture, '.next-ticket-code')).toEqual(['R-015', 'D-008', 'R-016']);
    expect(textsOf(fixture, '.next-ticket-service')).toEqual([
      'Registration',
      'Documents',
      'Registration',
    ]);
  });

  it('shows the more waiting count and the line about the counter', () => {
    fetchWaitingRoom.mockReturnValue(of(waitingRoomWithCalledAndIdleCounter));

    const fixture = createPage();

    expect(textsOf(fixture, '.more-waiting')).toEqual(['4 more waiting']);
    expect(textsOf(fixture, '.footer-note')).toEqual([
      'Go to the counter shown when your number appears.',
    ]);
  });

  it('shows the organization name and the Dor wordmark in the header', () => {
    fetchWaitingRoom.mockReturnValue(of(waitingRoomWithCalledAndIdleCounter));

    const fixture = createPage();

    expect(textsOf(fixture, '.organization-name')).toEqual(['Student Affairs office']);
    expect(pageElement(fixture).querySelector('.wordmark')?.getAttribute('aria-label')).toBe('Dor');
  });

  it('shows the empty state when no counter is open', () => {
    fetchWaitingRoom.mockReturnValue(
      of({ counters: [], nextTickets: [], moreWaitingCount: 0 } satisfies WaitingRoom),
    );

    const fixture = createPage();

    expect(textsOf(fixture, '.empty-state-title')).toEqual(['No counters are open right now']);
    expect(pageElement(fixture).querySelector('app-counter-tile')).toBeNull();
  });

  it('shows a message when the first load fails', () => {
    fetchWaitingRoom.mockReturnValue(failingRequestWithStatus(500));

    const fixture = createPage();

    expect(pageElement(fixture).querySelector('[role="alert"]')).not.toBeNull();
    expect(pageElement(fixture).querySelector('app-counter-tile')).toBeNull();
  });

  describe('live refresh', () => {
    afterEach(() => {
      vi.useRealTimers();
    });

    function requestRefresh(fixture: ComponentFixture<WaitingScreenPage>): void {
      fakeLiveUpdateService.liveUpdateWatch.requestRefresh();
      fixture.detectChanges();
    }

    it('watches the public stream', () => {
      fetchWaitingRoom.mockReturnValue(of(waitingRoomWithCalledAndIdleCounter));

      createPage();

      expect(fakeLiveUpdateService.watchedLiveStreams).toEqual(['public']);
    });

    it('constructor_beforeFirstLoadRequest_doesNotFetch', () => {
      fetchWaitingRoom.mockReturnValue(of(waitingRoomWithCalledAndIdleCounter));

      createPageBeforeFirstLoad();

      expect(fetchWaitingRoom).not.toHaveBeenCalled();
    });

    it('firstLoadRequest_whenEmitted_fetchesOnce', () => {
      fetchWaitingRoom.mockReturnValue(of(waitingRoomWithCalledAndIdleCounter));

      createPage();

      expect(fetchWaitingRoom).toHaveBeenCalledTimes(1);
    });

    it('does not load the waiting room again by itself over time', () => {
      vi.useFakeTimers();
      fetchWaitingRoom.mockReturnValue(of(waitingRoomWithCalledAndIdleCounter));
      createPage();

      vi.advanceTimersByTime(60000);

      expect(fetchWaitingRoom).toHaveBeenCalledTimes(1);
    });

    it('loads the waiting room again when a refresh is requested', () => {
      fetchWaitingRoom.mockReturnValue(of(waitingRoomWithCalledAndIdleCounter));
      const fixture = createPage();

      requestRefresh(fixture);

      expect(fetchWaitingRoom).toHaveBeenCalledTimes(2);
    });

    it('shows the new called ticket after a refresh request', () => {
      const waitingRoomWithNewCall: WaitingRoom = {
        ...waitingRoomWithCalledAndIdleCounter,
        counters: [
          { counterName: 'Counter 1', serviceName: 'Registration', ticketCode: 'R-015' },
          { counterName: 'Counter 3', serviceName: 'Documents', ticketCode: 'D-008' },
        ],
      };
      fetchWaitingRoom.mockReturnValueOnce(of(waitingRoomWithCalledAndIdleCounter));
      fetchWaitingRoom.mockReturnValueOnce(of(waitingRoomWithNewCall));
      const fixture = createPage();

      requestRefresh(fixture);

      expect(textsOf(fixture, '.ticket-code')).toEqual(['R-015', 'D-008']);
      expect(pageElement(fixture).querySelector('.not-serving')).toBeNull();
    });

    it('keeps showing the last waiting room when a refresh fails', () => {
      fetchWaitingRoom.mockReturnValueOnce(of(waitingRoomWithCalledAndIdleCounter));
      fetchWaitingRoom.mockReturnValueOnce(failingRequestWithStatus(500));
      const fixture = createPage();

      requestRefresh(fixture);

      expect(textsOf(fixture, '.ticket-code')).toEqual(['R-012']);
      expect(pageElement(fixture).querySelector('[role="alert"]')).toBeNull();
    });

    it('shows the waiting room on the next refresh after the first load failed', () => {
      fetchWaitingRoom.mockReturnValueOnce(failingRequestWithStatus(500));
      fetchWaitingRoom.mockReturnValueOnce(of(waitingRoomWithCalledAndIdleCounter));
      const fixture = createPage();
      expect(pageElement(fixture).querySelector('[role="alert"]')).not.toBeNull();

      requestRefresh(fixture);

      expect(pageElement(fixture).querySelector('[role="alert"]')).toBeNull();
      expect(textsOf(fixture, '.ticket-code')).toEqual(['R-012']);
    });

    it('ignores the answer of an older request when a newer refresh was requested', () => {
      const waitingRoomWithNewCall: WaitingRoom = {
        ...waitingRoomWithCalledAndIdleCounter,
        counters: [{ counterName: 'Counter 1', serviceName: 'Registration', ticketCode: 'R-099' }],
      };
      const slowAnswer = new Subject<WaitingRoom>();
      fetchWaitingRoom.mockReturnValueOnce(of(waitingRoomWithCalledAndIdleCounter));
      fetchWaitingRoom.mockReturnValueOnce(slowAnswer);
      fetchWaitingRoom.mockReturnValueOnce(of(waitingRoomWithNewCall));
      const fixture = createPage();

      requestRefresh(fixture);
      requestRefresh(fixture);
      slowAnswer.next(waitingRoomWithCalledAndIdleCounter);
      fixture.detectChanges();

      expect(textsOf(fixture, '.ticket-code')).toEqual(['R-099']);
    });

    it('does not load the waiting room after the page is destroyed', () => {
      fetchWaitingRoom.mockReturnValue(of(waitingRoomWithCalledAndIdleCounter));
      const fixture = createPage();

      fixture.destroy();
      fakeLiveUpdateService.liveUpdateWatch.requestRefresh();

      expect(fetchWaitingRoom).toHaveBeenCalledTimes(1);
    });
  });
});
