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
