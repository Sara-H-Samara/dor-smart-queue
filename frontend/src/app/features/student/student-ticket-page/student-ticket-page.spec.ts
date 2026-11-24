import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Subject, of } from 'rxjs';

import { LiveUpdateService } from '../../../core/live-updates/live-update.service';
import { ActiveService } from '../../../core/models/active-service.model';
import { ActiveTicket, TakenTicket } from '../../../core/models/ticket.model';
import { AuthService } from '../../../core/services/auth.service';
import { StudentCatalogService } from '../../../core/services/student-catalog.service';
import { TicketService } from '../../../core/services/ticket.service';
import { FakeLiveUpdateService } from '../../../testing/fake-live-update-service';
import { failingRequestWithMessage, failingRequestWithStatus } from '../../../testing/http-failure';
import { StudentTicketPage } from './student-ticket-page';

const NO_COUNTER_MESSAGE = 'No counter is open for Registration right now.';

const waitingTicket: ActiveTicket = {
  ticketCode: 'A-001',
  serviceName: 'Registration',
  status: 'WAITING',
  position: 3,
  peopleAhead: 2,
  estimatedWaitMinutes: 10,
  counterName: null,
  hasOpenCounter: true,
};

const openService: ActiveService = {
  id: 1,
  name: 'Registration',
  code: 'R',
  active: true,
  hasOpenCounter: true,
};

const closedService: ActiveService = { ...openService, hasOpenCounter: false };

const takenTicket: TakenTicket = {
  id: 7,
  ticketCode: 'A-001',
  serviceId: 1,
  serviceName: 'Registration',
  status: 'WAITING',
  createdAt: '2026-11-12T08:00:00Z',
};

describe('StudentTicketPage', () => {
  let fetchMyActiveTicket: ReturnType<typeof vi.fn>;
  let takeTicket: ReturnType<typeof vi.fn>;
  let cancelMyActiveTicket: ReturnType<typeof vi.fn>;
  let listActiveServices: ReturnType<typeof vi.fn>;
  let fakeLiveUpdateService: FakeLiveUpdateService;

  beforeEach(() => {
    fetchMyActiveTicket = vi.fn();
    takeTicket = vi.fn();
    cancelMyActiveTicket = vi.fn();
    listActiveServices = vi.fn();
    fakeLiveUpdateService = new FakeLiveUpdateService();

    TestBed.configureTestingModule({
      providers: [
        {
          provide: TicketService,
          useValue: { fetchMyActiveTicket, takeTicket, cancelMyActiveTicket },
        },
        { provide: StudentCatalogService, useValue: { listActiveServices } },
        { provide: LiveUpdateService, useValue: fakeLiveUpdateService },
        { provide: AuthService, useValue: { fullName: () => 'Sara Samara', logout: vi.fn() } },
        { provide: Router, useValue: { navigateByUrl: vi.fn() } },
      ],
    });
  });

  function createPageBeforeFirstLoad(): ComponentFixture<StudentTicketPage> {
    const fixture = TestBed.createComponent(StudentTicketPage);
    fixture.detectChanges();
    return fixture;
  }

  function createPage(): ComponentFixture<StudentTicketPage> {
    const fixture = createPageBeforeFirstLoad();
    fakeLiveUpdateService.liveUpdateWatch.requestFirstLoad();
    fixture.detectChanges();
    return fixture;
  }

  function pageElement(fixture: ComponentFixture<StudentTicketPage>): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function requestRefresh(fixture: ComponentFixture<StudentTicketPage>): void {
    fakeLiveUpdateService.liveUpdateWatch.requestRefresh();
    fixture.detectChanges();
  }

  function buttonWithText(
    fixture: ComponentFixture<StudentTicketPage>,
    text: string,
  ): HTMLButtonElement | undefined {
    const buttons = Array.from(pageElement(fixture).querySelectorAll('button'));
    return buttons.find((button) => button.textContent?.trim() === text);
  }

  it('shows one no-ticket heading when the student has no ticket', () => {
    fetchMyActiveTicket.mockReturnValue(failingRequestWithStatus(404));
    listActiveServices.mockReturnValue(of([openService]));

    const fixture = createPage();

    const headings = pageElement(fixture).querySelectorAll('h1, h2');
    expect(headings).toHaveLength(1);
    expect(headings[0].textContent?.trim()).toBe('You have no ticket');
  });

  it('shows the Your ticket heading when the student has a ticket', () => {
    fetchMyActiveTicket.mockReturnValue(of(waitingTicket));

    const fixture = createPage();

    expect(pageElement(fixture).querySelector('h1')?.textContent?.trim()).toBe('Your ticket');
  });

  it('shows the failed state when loading the ticket fails', () => {
    fetchMyActiveTicket.mockReturnValue(failingRequestWithStatus(500));

    const fixture = createPage();

    expect(pageElement(fixture).querySelector('[role="alert"]')).not.toBeNull();
    expect(buttonWithText(fixture, 'Try again')).toBeDefined();
  });

  it('shows the failed state without the service list when taking a ticket fails', () => {
    fetchMyActiveTicket.mockReturnValue(failingRequestWithStatus(404));
    listActiveServices.mockReturnValue(of([openService]));
    takeTicket.mockReturnValue(failingRequestWithStatus(500));
    const fixture = createPage();

    (pageElement(fixture).querySelector('.service-tile') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(buttonWithText(fixture, 'Try again')).toBeDefined();
    expect(pageElement(fixture).querySelector('.service-tile')).toBeNull();
  });

  it('shows the failed state without the ticket card when the reload after leaving fails', () => {
    fetchMyActiveTicket.mockReturnValueOnce(of(waitingTicket));
    cancelMyActiveTicket.mockReturnValue(of(undefined));
    fetchMyActiveTicket.mockReturnValueOnce(failingRequestWithStatus(500));
    const fixture = createPage();

    buttonWithText(fixture, 'Leave queue')?.click();
    fixture.detectChanges();

    expect(buttonWithText(fixture, 'Try again')).toBeDefined();
    expect(pageElement(fixture).querySelector('app-ticket-card')).toBeNull();
  });

  it('shows the failed state when the service list fails after leaving the queue', () => {
    fetchMyActiveTicket.mockReturnValueOnce(of(waitingTicket));
    cancelMyActiveTicket.mockReturnValue(of(undefined));
    fetchMyActiveTicket.mockReturnValueOnce(failingRequestWithStatus(404));
    listActiveServices.mockReturnValue(failingRequestWithStatus(500));
    const fixture = createPage();

    buttonWithText(fixture, 'Leave queue')?.click();
    fixture.detectChanges();

    expect(buttonWithText(fixture, 'Try again')).toBeDefined();
    expect(buttonWithText(fixture, 'Try again')?.disabled).toBe(false);
    expect(pageElement(fixture).querySelector('app-ticket-card')).toBeNull();
    expect(pageElement(fixture).querySelector('app-service-tile-list')).toBeNull();
  });

  it('shows the ticket again when the student retries after a failed load', () => {
    fetchMyActiveTicket.mockReturnValueOnce(failingRequestWithStatus(500));
    fetchMyActiveTicket.mockReturnValueOnce(of(waitingTicket));
    const fixture = createPage();

    buttonWithText(fixture, 'Try again')?.click();
    fixture.detectChanges();

    expect(pageElement(fixture).querySelector('app-ticket-card')).not.toBeNull();
    expect(pageElement(fixture).querySelector('[role="alert"]')).toBeNull();
  });

  it('shows the ticket card after a ticket is taken', () => {
    fetchMyActiveTicket.mockReturnValueOnce(failingRequestWithStatus(404));
    listActiveServices.mockReturnValue(of([openService]));
    takeTicket.mockReturnValue(of(takenTicket));
    fetchMyActiveTicket.mockReturnValueOnce(of(waitingTicket));
    const fixture = createPage();

    (pageElement(fixture).querySelector('.service-tile') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(pageElement(fixture).querySelector('app-ticket-card')).not.toBeNull();
  });

  it('shows the server message above the service list on a conflict without a ticket', () => {
    fetchMyActiveTicket.mockReturnValue(failingRequestWithStatus(404));
    listActiveServices.mockReturnValueOnce(of([openService]));
    listActiveServices.mockReturnValueOnce(of([closedService]));
    takeTicket.mockReturnValue(failingRequestWithMessage(409, NO_COUNTER_MESSAGE));
    const fixture = createPage();

    (pageElement(fixture).querySelector('.service-tile') as HTMLButtonElement).click();
    fixture.detectChanges();

    const alertElement = pageElement(fixture).querySelector('p.alert[role="alert"]');
    const tileListElement = pageElement(fixture).querySelector('app-service-tile-list');
    expect(alertElement?.textContent?.trim()).toBe(NO_COUNTER_MESSAGE);
    expect(tileListElement?.textContent).toContain('No counter open right now');
    expect(alertElement?.compareDocumentPosition(tileListElement as Node)).toBe(
      Node.DOCUMENT_POSITION_FOLLOWING,
    );
  });

  it('shows the ticket without a message on a conflict when the student has a ticket', () => {
    fetchMyActiveTicket.mockReturnValueOnce(failingRequestWithStatus(404));
    fetchMyActiveTicket.mockReturnValueOnce(of(waitingTicket));
    listActiveServices.mockReturnValue(of([openService]));
    takeTicket.mockReturnValue(failingRequestWithMessage(409, NO_COUNTER_MESSAGE));
    const fixture = createPage();

    (pageElement(fixture).querySelector('.service-tile') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(pageElement(fixture).querySelector('app-ticket-card')).not.toBeNull();
    expect(pageElement(fixture).querySelector('[role="alert"]')).toBeNull();
    expect(pageElement(fixture).textContent).not.toContain(NO_COUNTER_MESSAGE);
  });

  it('removes the old conflict message when the student takes the next ticket', () => {
    fetchMyActiveTicket.mockReturnValue(failingRequestWithStatus(404));
    listActiveServices.mockReturnValue(of([openService]));
    takeTicket.mockReturnValueOnce(failingRequestWithMessage(409, NO_COUNTER_MESSAGE));
    takeTicket.mockReturnValueOnce(new Subject<TakenTicket>());
    const fixture = createPage();
    (pageElement(fixture).querySelector('.service-tile') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(pageElement(fixture).querySelector('[role="alert"]')).not.toBeNull();

    (pageElement(fixture).querySelector('.service-tile') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(pageElement(fixture).querySelector('[role="alert"]')).toBeNull();
  });

  it('shows the service list without the conflict message when retrying after a failed reload', () => {
    fetchMyActiveTicket.mockReturnValueOnce(failingRequestWithStatus(404));
    fetchMyActiveTicket.mockReturnValueOnce(failingRequestWithStatus(500));
    fetchMyActiveTicket.mockReturnValueOnce(failingRequestWithStatus(404));
    listActiveServices.mockReturnValue(of([openService]));
    takeTicket.mockReturnValue(failingRequestWithMessage(409, NO_COUNTER_MESSAGE));
    const fixture = createPage();
    (pageElement(fixture).querySelector('.service-tile') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(buttonWithText(fixture, 'Try again')).toBeDefined();

    buttonWithText(fixture, 'Try again')?.click();
    fixture.detectChanges();

    expect(pageElement(fixture).querySelector('app-service-tile-list')).not.toBeNull();
    expect(pageElement(fixture).querySelector('[role="alert"]')).toBeNull();
  });

  describe('live refresh', () => {
    afterEach(() => {
      vi.useRealTimers();
    });

    it('watches the student stream', () => {
      fetchMyActiveTicket.mockReturnValue(of(waitingTicket));

      createPage();

      expect(fakeLiveUpdateService.watchedLiveStreams).toEqual(['student']);
    });

    it('constructor_beforeFirstLoadRequest_doesNotFetchAnything', () => {
      fetchMyActiveTicket.mockReturnValue(of(waitingTicket));

      createPageBeforeFirstLoad();

      expect(fetchMyActiveTicket).not.toHaveBeenCalled();
      expect(listActiveServices).not.toHaveBeenCalled();
    });

    it('firstLoadRequest_whenStudentHasNoTicket_runsTicketThenServicesOnce', () => {
      fetchMyActiveTicket.mockReturnValue(failingRequestWithStatus(404));
      listActiveServices.mockReturnValue(of([openService]));

      const fixture = createPage();

      expect(fetchMyActiveTicket).toHaveBeenCalledTimes(1);
      expect(listActiveServices).toHaveBeenCalledTimes(1);
      expect(pageElement(fixture).querySelector('app-service-tile-list')).not.toBeNull();
    });

    it('firstLoadRequest_whenStudentHasTicket_fetchesTheTicketOnce', () => {
      fetchMyActiveTicket.mockReturnValue(of(waitingTicket));

      createPage();

      expect(fetchMyActiveTicket).toHaveBeenCalledTimes(1);
      expect(listActiveServices).not.toHaveBeenCalled();
    });

    it('does not load anything again by itself over time', () => {
      vi.useFakeTimers();
      fetchMyActiveTicket.mockReturnValue(of(waitingTicket));
      createPage();

      vi.advanceTimersByTime(60000);

      expect(fetchMyActiveTicket).toHaveBeenCalledTimes(1);
      expect(listActiveServices).not.toHaveBeenCalled();
    });

    it('shows the no-counter notice when the counter turns off', () => {
      const ticketWithoutOpenCounter: ActiveTicket = {
        ...waitingTicket,
        estimatedWaitMinutes: null,
        hasOpenCounter: false,
      };
      fetchMyActiveTicket.mockReturnValueOnce(of(waitingTicket));
      fetchMyActiveTicket.mockReturnValueOnce(of(ticketWithoutOpenCounter));
      const fixture = createPage();
      expect(pageElement(fixture).textContent).toContain('About 10 min');

      requestRefresh(fixture);

      expect(pageElement(fixture).textContent).toContain('No counter is open right now');
      expect(pageElement(fixture).textContent).toContain('Your place is kept.');
      expect(pageElement(fixture).textContent).not.toContain('About');
      expect(pageElement(fixture).querySelector('[role="status"]')).toBeNull();
    });

    it('shows the service list without the old conflict message when the ticket is gone', () => {
      fetchMyActiveTicket.mockReturnValueOnce(failingRequestWithStatus(404));
      fetchMyActiveTicket.mockReturnValueOnce(of(waitingTicket));
      fetchMyActiveTicket.mockReturnValueOnce(failingRequestWithStatus(404));
      listActiveServices.mockReturnValue(of([openService]));
      takeTicket.mockReturnValue(failingRequestWithMessage(409, NO_COUNTER_MESSAGE));
      const fixture = createPage();
      (pageElement(fixture).querySelector('.service-tile') as HTMLButtonElement).click();
      fixture.detectChanges();
      expect(pageElement(fixture).querySelector('app-ticket-card')).not.toBeNull();

      requestRefresh(fixture);

      expect(pageElement(fixture).querySelector('app-service-tile-list')).not.toBeNull();
      expect(pageElement(fixture).querySelector('[role="alert"]')).toBeNull();
    });

    it('updates the ticket after a refresh request without showing the loading state', () => {
      fetchMyActiveTicket.mockReturnValueOnce(of(waitingTicket));
      fetchMyActiveTicket.mockReturnValueOnce(of({ ...waitingTicket, status: 'CALLED' }));
      const fixture = createPage();

      requestRefresh(fixture);

      expect(pageElement(fixture).textContent).toContain('Your turn');
      expect(pageElement(fixture).querySelector('[role="status"]')).toBeNull();
      expect(fetchMyActiveTicket).toHaveBeenCalledTimes(2);
    });

    it('uses only the newest answer when refreshes are requested close together', () => {
      const slowAnswer = new Subject<ActiveTicket>();
      fetchMyActiveTicket.mockReturnValueOnce(of(waitingTicket));
      fetchMyActiveTicket.mockReturnValueOnce(slowAnswer);
      fetchMyActiveTicket.mockReturnValueOnce(of({ ...waitingTicket, status: 'CALLED' }));
      const fixture = createPage();

      requestRefresh(fixture);
      requestRefresh(fixture);
      slowAnswer.next(waitingTicket);
      fixture.detectChanges();

      expect(pageElement(fixture).textContent).toContain('Your turn');
      expect(fetchMyActiveTicket).toHaveBeenCalledTimes(3);
    });

    it('does not ask for the ticket when the student has no ticket', () => {
      fetchMyActiveTicket.mockReturnValue(failingRequestWithStatus(404));
      listActiveServices.mockReturnValue(of([openService]));
      const fixture = createPage();

      requestRefresh(fixture);

      expect(fetchMyActiveTicket).toHaveBeenCalledTimes(1);
    });

    it('sends no request in the failed state', () => {
      fetchMyActiveTicket.mockReturnValue(failingRequestWithStatus(500));
      const fixture = createPage();

      requestRefresh(fixture);

      expect(fetchMyActiveTicket).toHaveBeenCalledTimes(1);
      expect(listActiveServices).not.toHaveBeenCalled();
    });

    it('sends no request while the ticket is being left', () => {
      fetchMyActiveTicket.mockReturnValue(of(waitingTicket));
      cancelMyActiveTicket.mockReturnValue(new Subject<void>());
      const fixture = createPage();
      buttonWithText(fixture, 'Leave queue')?.click();

      requestRefresh(fixture);

      expect(fetchMyActiveTicket).toHaveBeenCalledTimes(1);
    });

    it('sends no request when the ticket is already finished', () => {
      fetchMyActiveTicket.mockReturnValue(of({ ...waitingTicket, status: 'DONE' }));
      const fixture = createPage();

      requestRefresh(fixture);

      expect(fetchMyActiveTicket).toHaveBeenCalledTimes(1);
    });

    it('shows the service list when the ticket is gone', () => {
      fetchMyActiveTicket.mockReturnValueOnce(of(waitingTicket));
      fetchMyActiveTicket.mockReturnValueOnce(failingRequestWithStatus(404));
      listActiveServices.mockReturnValue(of([openService]));
      const fixture = createPage();

      requestRefresh(fixture);

      expect(pageElement(fixture).querySelector('app-service-tile-list')).not.toBeNull();
      expect(pageElement(fixture).querySelector('app-ticket-card')).toBeNull();
    });

    it('shows loading until the services arrive when the ticket is gone', () => {
      const pendingServices = new Subject<ActiveService[]>();
      fetchMyActiveTicket.mockReturnValueOnce(of(waitingTicket));
      fetchMyActiveTicket.mockReturnValueOnce(failingRequestWithStatus(404));
      listActiveServices.mockReturnValue(pendingServices);
      const fixture = createPage();

      requestRefresh(fixture);
      expect(pageElement(fixture).querySelector('[role="status"]')).not.toBeNull();

      pendingServices.next([openService]);
      fixture.detectChanges();

      expect(pageElement(fixture).querySelector('[role="status"]')).toBeNull();
      expect(pageElement(fixture).querySelector('app-service-tile-list')).not.toBeNull();
    });

    it('keeps the ticket and refreshes again at the next request when a refresh fails', () => {
      fetchMyActiveTicket.mockReturnValueOnce(of(waitingTicket));
      fetchMyActiveTicket.mockReturnValueOnce(failingRequestWithStatus(500));
      fetchMyActiveTicket.mockReturnValueOnce(of({ ...waitingTicket, status: 'CALLED' }));
      const fixture = createPage();

      requestRefresh(fixture);
      expect(pageElement(fixture).querySelector('app-ticket-card')).not.toBeNull();
      expect(pageElement(fixture).querySelector('[role="alert"]')).toBeNull();

      requestRefresh(fixture);

      expect(pageElement(fixture).textContent).toContain('Your turn');
      expect(fetchMyActiveTicket).toHaveBeenCalledTimes(3);
    });

    it('ignores a late answer after the student leaves the queue', () => {
      const lateRefreshAnswer = new Subject<ActiveTicket>();
      fetchMyActiveTicket.mockReturnValueOnce(of(waitingTicket));
      fetchMyActiveTicket.mockReturnValueOnce(lateRefreshAnswer);
      fetchMyActiveTicket.mockReturnValueOnce(failingRequestWithStatus(404));
      cancelMyActiveTicket.mockReturnValue(of(undefined));
      listActiveServices.mockReturnValue(of([openService]));
      const fixture = createPage();
      requestRefresh(fixture);

      buttonWithText(fixture, 'Leave queue')?.click();
      lateRefreshAnswer.next(waitingTicket);
      fixture.detectChanges();

      expect(pageElement(fixture).querySelector('app-ticket-card')).toBeNull();
      expect(pageElement(fixture).querySelector('app-service-tile-list')).not.toBeNull();
    });

    it('sends no request after the page is destroyed', () => {
      fetchMyActiveTicket.mockReturnValue(of(waitingTicket));
      const fixture = createPage();

      fixture.destroy();
      fakeLiveUpdateService.liveUpdateWatch.requestRefresh();

      expect(fetchMyActiveTicket).toHaveBeenCalledTimes(1);
    });
  });

  describe('live refresh of the service list', () => {
    beforeEach(() => {
      fetchMyActiveTicket.mockReturnValue(failingRequestWithStatus(404));
    });

    it('updates the tiles after a refresh request', () => {
      listActiveServices.mockReturnValueOnce(of([openService]));
      listActiveServices.mockReturnValueOnce(of([closedService]));
      const fixture = createPage();
      expect(pageElement(fixture).textContent).not.toContain('No counter open right now');

      requestRefresh(fixture);

      expect(pageElement(fixture).textContent).toContain('No counter open right now');
      expect(pageElement(fixture).querySelector('[role="status"]')).toBeNull();
      expect(listActiveServices).toHaveBeenCalledTimes(2);
    });

    it('uses only the newest answer when refreshes are requested close together', () => {
      const slowAnswer = new Subject<ActiveService[]>();
      listActiveServices.mockReturnValueOnce(of([openService]));
      listActiveServices.mockReturnValueOnce(slowAnswer);
      listActiveServices.mockReturnValueOnce(of([closedService]));
      const fixture = createPage();

      requestRefresh(fixture);
      requestRefresh(fixture);
      slowAnswer.next([openService]);
      fixture.detectChanges();

      expect(pageElement(fixture).textContent).toContain('No counter open right now');
      expect(listActiveServices).toHaveBeenCalledTimes(3);
    });

    it('keeps the list and refreshes again at the next request when a refresh fails', () => {
      listActiveServices.mockReturnValueOnce(of([openService]));
      listActiveServices.mockReturnValueOnce(failingRequestWithStatus(500));
      listActiveServices.mockReturnValueOnce(of([closedService]));
      const fixture = createPage();

      requestRefresh(fixture);
      expect(pageElement(fixture).querySelector('.service-tile')).not.toBeNull();
      expect(pageElement(fixture).querySelector('[role="alert"]')).toBeNull();

      requestRefresh(fixture);

      expect(pageElement(fixture).textContent).toContain('No counter open right now');
      expect(listActiveServices).toHaveBeenCalledTimes(3);
    });

    it('sends no request while a ticket is being taken', () => {
      listActiveServices.mockReturnValue(of([openService]));
      takeTicket.mockReturnValue(new Subject<TakenTicket>());
      const fixture = createPage();
      (pageElement(fixture).querySelector('.service-tile') as HTMLButtonElement).click();

      requestRefresh(fixture);

      expect(listActiveServices).toHaveBeenCalledTimes(1);
    });

    it('sends no request for the service list when the student has a ticket', () => {
      fetchMyActiveTicket.mockReturnValue(of(waitingTicket));
      const fixture = createPage();

      requestRefresh(fixture);

      expect(listActiveServices).not.toHaveBeenCalled();
    });

    it('sends no request after the page is destroyed', () => {
      listActiveServices.mockReturnValue(of([openService]));
      const fixture = createPage();

      fixture.destroy();
      fakeLiveUpdateService.liveUpdateWatch.requestRefresh();

      expect(listActiveServices).toHaveBeenCalledTimes(1);
    });

    it('ignores a late answer after the service list is reloaded', () => {
      const lateRefreshAnswer = new Subject<ActiveService[]>();
      listActiveServices.mockReturnValueOnce(of([openService]));
      listActiveServices.mockReturnValueOnce(lateRefreshAnswer);
      listActiveServices.mockReturnValueOnce(of([closedService]));
      takeTicket.mockReturnValue(failingRequestWithMessage(409, NO_COUNTER_MESSAGE));
      const fixture = createPage();
      requestRefresh(fixture);

      (pageElement(fixture).querySelector('.service-tile') as HTMLButtonElement).click();
      lateRefreshAnswer.next([openService]);
      fixture.detectChanges();

      expect(pageElement(fixture).textContent).toContain('No counter open right now');
    });
  });
});
