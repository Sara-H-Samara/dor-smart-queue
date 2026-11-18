import { TestBed } from '@angular/core/testing';

import { ActiveTicket } from '../../../../core/models/ticket.model';
import { TicketCard } from './ticket-card';

describe('TicketCard', () => {
  function ticketWithStatus(status: 'WAITING' | 'CALLED', counterName: string | null): ActiveTicket {
    return {
      ticketCode: 'A-009',
      serviceName: 'Registration',
      status,
      position: null,
      peopleAhead: null,
      estimatedWaitMinutes: null,
      counterName,
      hasOpenCounter: true,
    };
  }

  function renderTicketCard(ticket: ActiveTicket): HTMLElement {
    const fixture = TestBed.createComponent(TicketCard);
    fixture.componentRef.setInput('ticket', ticket);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('shows no count and no estimate when people ahead and wait time are unknown', () => {
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

  it('shows the wait in minutes without the word estimate or a footer', () => {
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

  it('shows the no-counter text and no wait time when no counter is open', () => {
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

  it('shows the ticket details and the leave-queue button when no counter is open', () => {
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

  it('ignores the open-counter flag for a called ticket', () => {
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

  it('shows only Your turn in the heading when the called ticket has no counter name', () => {
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

  it('shows the counter name only in the heading when the called ticket has a counter name', () => {
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
      'Please go to your counter.',
    );
    expect(cardElement.textContent).not.toContain('Please go there now.');
    expect(cardElement.textContent?.match(/Counter 2/g)).toHaveLength(1);
  });

  it('shows an arrow icon before the stub sentence of a called ticket', () => {
    const cardElement = renderTicketCard(ticketWithStatus('CALLED', 'Counter 2'));

    const stubParagraph = cardElement.querySelector('.ticket-stub p');

    expect(stubParagraph?.firstElementChild?.tagName.toLowerCase()).toBe('app-icon');
    expect(stubParagraph?.querySelector('app-icon svg')).not.toBeNull();
  });

  it('hides the stub arrow from screen readers', () => {
    const cardElement = renderTicketCard(ticketWithStatus('CALLED', null));

    const arrowIcon = cardElement.querySelector('.ticket-stub p app-icon');

    expect(arrowIcon?.getAttribute('aria-hidden')).toBe('true');
  });

  it('shows the leave button and no stub sentence while the ticket is waiting', () => {
    const cardElement = renderTicketCard(ticketWithStatus('WAITING', null));

    expect(cardElement.querySelector('.ticket-stub button')?.textContent?.trim()).toBe(
      'Leave queue',
    );
    expect(cardElement.querySelector('.ticket-stub p')).toBeNull();
    expect(cardElement.textContent).not.toContain('Please go to your counter.');
  });
});
