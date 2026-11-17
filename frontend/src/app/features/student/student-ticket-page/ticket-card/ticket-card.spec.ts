import { TestBed } from '@angular/core/testing';

import { ActiveTicket } from '../../../../core/models/ticket.model';
import { TicketCard } from './ticket-card';

describe('TicketCard', () => {
  it('render_withNullPeopleAheadAndWait_showsNoCountOrEstimate', () => {
    const ticketWithoutQueuePosition: ActiveTicket = {
      ticketCode: 'A-001',
      serviceName: 'Registration',
      status: 'WAITING',
      position: null,
      peopleAhead: null,
      estimatedWaitMinutes: null,
      counterName: null,
      hasOpenCounter: true,
    };
    const fixture = TestBed.createComponent(TicketCard);
    fixture.componentRef.setInput('ticket', ticketWithoutQueuePosition);

    fixture.detectChanges();

    const cardElement = fixture.nativeElement as HTMLElement;
    expect(cardElement.querySelector('.ticket-status')?.textContent?.trim()).toBe('');
    expect(cardElement.querySelector('.state-chip')?.textContent?.trim()).toBe('Waiting');
  });

  it('render_withWaitingTicketAndWait_showsMinutesWithoutEstimateWordOrFooter', () => {
    const waitingTicket: ActiveTicket = {
      ticketCode: 'A-002',
      serviceName: 'Registration',
      status: 'WAITING',
      position: 3,
      peopleAhead: 2,
      estimatedWaitMinutes: 5,
      counterName: null,
      hasOpenCounter: true,
    };
    const fixture = TestBed.createComponent(TicketCard);
    fixture.componentRef.setInput('ticket', waitingTicket);

    fixture.detectChanges();

    const cardElement = fixture.nativeElement as HTMLElement;
    expect(cardElement.querySelector('.ticket-status')?.textContent).toContain('About 5 min');
    expect(cardElement.textContent).not.toContain('estimate');
    expect(cardElement.textContent).not.toContain('Estimates can change');
  });

  it('render_waitingWithoutOpenCounter_showsNoCounterTextAndNoWaitTime', () => {
    const waitingTicketWithoutOpenCounter: ActiveTicket = {
      ticketCode: 'A-005',
      serviceName: 'Registration',
      status: 'WAITING',
      position: 1,
      peopleAhead: 0,
      estimatedWaitMinutes: null,
      counterName: null,
      hasOpenCounter: false,
    };
    const fixture = TestBed.createComponent(TicketCard);
    fixture.componentRef.setInput('ticket', waitingTicketWithoutOpenCounter);

    fixture.detectChanges();

    const statusElement = (fixture.nativeElement as HTMLElement).querySelector('.ticket-status');
    expect(statusElement?.querySelector('b')?.textContent?.trim()).toBe(
      'No counter is open right now',
    );
    expect(statusElement?.querySelector('span')?.textContent?.trim()).toBe('Your place is kept.');
    expect(statusElement?.textContent).not.toContain('You are next');
    expect(statusElement?.textContent).not.toContain('About');
    expect(statusElement?.querySelector('app-icon')).toBeNull();
  });

  it('render_waitingWithoutOpenCounter_keepsLeaveQueueButton', () => {
    const waitingTicketWithoutOpenCounter: ActiveTicket = {
      ticketCode: 'A-006',
      serviceName: 'Registration',
      status: 'WAITING',
      position: 2,
      peopleAhead: 1,
      estimatedWaitMinutes: null,
      counterName: null,
      hasOpenCounter: false,
    };
    const fixture = TestBed.createComponent(TicketCard);
    fixture.componentRef.setInput('ticket', waitingTicketWithoutOpenCounter);

    fixture.detectChanges();

    const cardElement = fixture.nativeElement as HTMLElement;
    expect(cardElement.querySelector('.service-name')?.textContent?.trim()).toBe('Registration');
    expect(cardElement.querySelector('.ticket-code')?.textContent?.trim()).toBe('A-006');
    expect(cardElement.querySelector('.state-chip')?.textContent?.trim()).toBe('Waiting');
    expect(cardElement.querySelector('.ticket-stub button')?.textContent?.trim()).toBe(
      'Leave queue',
    );
  });

  it('render_calledTicket_ignoresHasOpenCounter', () => {
    const calledTicketWithFlagFalse: ActiveTicket = {
      ticketCode: 'A-007',
      serviceName: 'Registration',
      status: 'CALLED',
      position: null,
      peopleAhead: null,
      estimatedWaitMinutes: null,
      counterName: 'Counter 2',
      hasOpenCounter: false,
    };
    const fixture = TestBed.createComponent(TicketCard);
    fixture.componentRef.setInput('ticket', calledTicketWithFlagFalse);

    fixture.detectChanges();

    const cardElement = fixture.nativeElement as HTMLElement;
    expect(cardElement.querySelector('.ticket-status')?.textContent?.trim()).toBe(
      'Your turn: Counter 2',
    );
    expect(cardElement.textContent).not.toContain('No counter is open');
  });

  it('render_withCalledTicketWithoutCounterName_showsYourTurnAndGenericCounterLine', () => {
    const calledTicket: ActiveTicket = {
      ticketCode: 'A-003',
      serviceName: 'Registration',
      status: 'CALLED',
      position: 0,
      peopleAhead: 0,
      estimatedWaitMinutes: 0,
      counterName: null,
      hasOpenCounter: true,
    };
    const fixture = TestBed.createComponent(TicketCard);
    fixture.componentRef.setInput('ticket', calledTicket);

    fixture.detectChanges();

    const cardElement = fixture.nativeElement as HTMLElement;
    expect(cardElement.querySelector('.ticket-status')?.textContent?.trim()).toBe('Your turn');
    expect(cardElement.querySelector('.ticket-stub')?.textContent?.trim()).toBe(
      'Please go to your counter.',
    );
    expect(cardElement.textContent).not.toContain('Show this number');
    expect(cardElement.textContent).not.toContain('Your place is held');
  });

  it('render_withCalledTicketAndCounterName_showsCounterOnlyInHeading', () => {
    const calledTicketWithCounter: ActiveTicket = {
      ticketCode: 'A-004',
      serviceName: 'Registration',
      status: 'CALLED',
      position: null,
      peopleAhead: null,
      estimatedWaitMinutes: null,
      counterName: 'Counter 2',
      hasOpenCounter: true,
    };
    const fixture = TestBed.createComponent(TicketCard);
    fixture.componentRef.setInput('ticket', calledTicketWithCounter);

    fixture.detectChanges();

    const cardElement = fixture.nativeElement as HTMLElement;
    expect(cardElement.querySelector('.ticket-status')?.textContent?.trim()).toBe(
      'Your turn: Counter 2',
    );
    expect(cardElement.querySelector('.ticket-stub')?.textContent?.trim()).toBe(
      'Please go there now.',
    );
    expect(cardElement.textContent).not.toContain('Please go to your counter.');
    expect(cardElement.textContent?.match(/Counter 2/g)).toHaveLength(1);
  });
});
