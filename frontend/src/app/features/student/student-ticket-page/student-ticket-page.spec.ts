import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';

import { ActiveService } from '../../../core/models/active-service.model';
import { ActiveTicket, TakenTicket } from '../../../core/models/ticket.model';
import { AuthService } from '../../../core/services/auth.service';
import { StudentCatalogService } from '../../../core/services/student-catalog.service';
import { TicketService } from '../../../core/services/ticket.service';
import { failingRequestWithStatus } from '../../../testing/http-failure';
import { StudentTicketPage } from './student-ticket-page';

const waitingTicket: ActiveTicket = {
  ticketCode: 'A-001',
  serviceName: 'Registration',
  status: 'WAITING',
  position: 3,
  peopleAhead: 2,
  estimatedWaitMinutes: 10,
};

const openService: ActiveService = { id: 1, name: 'Registration', code: 'R', active: true };

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

  beforeEach(() => {
    fetchMyActiveTicket = vi.fn();
    takeTicket = vi.fn();
    cancelMyActiveTicket = vi.fn();
    listActiveServices = vi.fn();

    TestBed.configureTestingModule({
      providers: [
        {
          provide: TicketService,
          useValue: { fetchMyActiveTicket, takeTicket, cancelMyActiveTicket },
        },
        { provide: StudentCatalogService, useValue: { listActiveServices } },
        { provide: AuthService, useValue: { fullName: () => 'Sara Samara', logout: vi.fn() } },
        { provide: Router, useValue: { navigateByUrl: vi.fn() } },
      ],
    });
  });

  function createPage(): ComponentFixture<StudentTicketPage> {
    const fixture = TestBed.createComponent(StudentTicketPage);
    fixture.detectChanges();
    return fixture;
  }

  function pageElement(fixture: ComponentFixture<StudentTicketPage>): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function buttonWithText(
    fixture: ComponentFixture<StudentTicketPage>,
    text: string,
  ): HTMLButtonElement | undefined {
    const buttons = Array.from(pageElement(fixture).querySelectorAll('button'));
    return buttons.find((button) => button.textContent?.trim() === text);
  }

  it('loadMyActiveTicket_whenRequestFails_showsFailedState', () => {
    fetchMyActiveTicket.mockReturnValue(failingRequestWithStatus(500));

    const fixture = createPage();

    expect(pageElement(fixture).querySelector('[role="alert"]')).not.toBeNull();
    expect(buttonWithText(fixture, 'Try again')).toBeDefined();
  });

  it('takeTicketForService_whenRequestFails_showsFailedStateWithoutServiceList', () => {
    fetchMyActiveTicket.mockReturnValue(failingRequestWithStatus(404));
    listActiveServices.mockReturnValue(of([openService]));
    takeTicket.mockReturnValue(failingRequestWithStatus(500));
    const fixture = createPage();

    (pageElement(fixture).querySelector('.service-tile') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(buttonWithText(fixture, 'Try again')).toBeDefined();
    expect(pageElement(fixture).querySelector('.service-tile')).toBeNull();
  });

  it('leaveQueue_whenReloadFails_showsFailedStateWithoutTicketCard', () => {
    fetchMyActiveTicket.mockReturnValueOnce(of(waitingTicket));
    cancelMyActiveTicket.mockReturnValue(of(undefined));
    fetchMyActiveTicket.mockReturnValueOnce(failingRequestWithStatus(500));
    const fixture = createPage();

    buttonWithText(fixture, 'Leave queue')?.click();
    fixture.detectChanges();

    expect(buttonWithText(fixture, 'Try again')).toBeDefined();
    expect(pageElement(fixture).querySelector('app-ticket-card')).toBeNull();
  });

  it('leaveQueue_whenServiceListFails_showsFailedState', () => {
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

  it('retryAfterFailure_afterFailedLoad_showsTicketAgain', () => {
    fetchMyActiveTicket.mockReturnValueOnce(failingRequestWithStatus(500));
    fetchMyActiveTicket.mockReturnValueOnce(of(waitingTicket));
    const fixture = createPage();

    buttonWithText(fixture, 'Try again')?.click();
    fixture.detectChanges();

    expect(pageElement(fixture).querySelector('app-ticket-card')).not.toBeNull();
    expect(pageElement(fixture).querySelector('[role="alert"]')).toBeNull();
  });

  it('takeTicketForService_whenRequestSucceeds_showsTicketCard', () => {
    fetchMyActiveTicket.mockReturnValueOnce(failingRequestWithStatus(404));
    listActiveServices.mockReturnValue(of([openService]));
    takeTicket.mockReturnValue(of(takenTicket));
    fetchMyActiveTicket.mockReturnValueOnce(of(waitingTicket));
    const fixture = createPage();

    (pageElement(fixture).querySelector('.service-tile') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(pageElement(fixture).querySelector('app-ticket-card')).not.toBeNull();
  });
});
