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
      distance: 'NEAR',
      turnIsNear: false,
      alertAtPeopleAhead: null,
    };
  }

  function waitingTicketWith(overrides: Partial<ActiveTicket>): ActiveTicket {
    return { ...ticketWithStatus('WAITING', null), ...overrides };
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
      distance: 'NEAR',
      turnIsNear: false,
      alertAtPeopleAhead: null,
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
      distance: 'NEAR',
      turnIsNear: false,
      alertAtPeopleAhead: null,
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
      distance: 'NEAR',
      turnIsNear: false,
      alertAtPeopleAhead: null,
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
      distance: 'NEAR',
      turnIsNear: false,
      alertAtPeopleAhead: null,
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
      distance: 'NEAR',
      turnIsNear: false,
      alertAtPeopleAhead: null,
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
      distance: 'NEAR',
      turnIsNear: false,
      alertAtPeopleAhead: null,
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
      distance: 'NEAR',
      turnIsNear: false,
      alertAtPeopleAhead: null,
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

  describe('turn-near notice', () => {
    it('shows the notice when the server says the turn is near', () => {
      const cardElement = renderTicketCard(waitingTicketWith({ turnIsNear: true }));

      expect(cardElement.querySelector('.turn-notice')?.textContent).toContain(
        'Your turn is near. Please come to the counters.',
      );
    });

    it('announces the notice through a status region', () => {
      const cardElement = renderTicketCard(waitingTicketWith({ turnIsNear: true }));

      expect(cardElement.querySelector('.turn-notice')?.getAttribute('role')).toBe('status');
    });

    it('shows no notice when the server says the turn is not near', () => {
      const cardElement = renderTicketCard(waitingTicketWith({ turnIsNear: false }));

      expect(cardElement.querySelector('.turn-notice')).toBeNull();
      expect(cardElement.querySelector('[role="status"]')).toBeNull();
    });

    it('shows no notice for a called ticket even when the flag is true', () => {
      const calledTicket: ActiveTicket = {
        ...ticketWithStatus('CALLED', 'Counter 2'),
        turnIsNear: true,
      };

      const cardElement = renderTicketCard(calledTicket);

      expect(cardElement.querySelector('.turn-notice')).toBeNull();
    });

    it('shows no notice for zero people ahead when the server says the turn is not near', () => {
      const cardElement = renderTicketCard(
        waitingTicketWith({ position: 1, peopleAhead: 0, turnIsNear: false }),
      );

      expect(cardElement.textContent).toContain('You are next');
      expect(cardElement.querySelector('.turn-notice')).toBeNull();
    });

    it('shows the notice for many people ahead when the server says the turn is near', () => {
      const cardElement = renderTicketCard(
        waitingTicketWith({ position: 10, peopleAhead: 9, turnIsNear: true }),
      );

      expect(cardElement.querySelector('.turn-notice')).not.toBeNull();
    });

    it('keeps the leave-queue button when the notice is shown', () => {
      const cardElement = renderTicketCard(waitingTicketWith({ turnIsNear: true }));

      expect(cardElement.querySelector('.ticket-stub button')?.textContent?.trim()).toBe(
        'Leave queue',
      );
    });
  });

  describe('alert promise', () => {
    it('tells the student when the alert will come', () => {
      const cardElement = renderTicketCard(
        waitingTicketWith({ position: 8, peopleAhead: 7, alertAtPeopleAhead: 6 }),
      );

      expect(cardElement.querySelector('.ticket-status')?.textContent).toContain(
        'We will tell you when 6 people are ahead',
      );
    });

    it('shows no promise when the server sent no alert number', () => {
      const cardElement = renderTicketCard(waitingTicketWith({ alertAtPeopleAhead: null }));

      expect(cardElement.textContent).not.toContain('We will tell you');
    });

    it('shows no promise once the turn is near', () => {
      const cardElement = renderTicketCard(
        waitingTicketWith({ turnIsNear: true, alertAtPeopleAhead: 6 }),
      );

      expect(cardElement.textContent).not.toContain('We will tell you');
    });

    it('shows no promise when no counter is open', () => {
      const cardElement = renderTicketCard(
        waitingTicketWith({ hasOpenCounter: false, alertAtPeopleAhead: 6 }),
      );

      expect(cardElement.textContent).not.toContain('We will tell you');
    });

    it('shows no promise for a called ticket', () => {
      const calledTicket: ActiveTicket = {
        ...ticketWithStatus('CALLED', 'Counter 2'),
        alertAtPeopleAhead: 6,
      };

      const cardElement = renderTicketCard(calledTicket);

      expect(cardElement.textContent).not.toContain('We will tell you');
    });
  });
});
