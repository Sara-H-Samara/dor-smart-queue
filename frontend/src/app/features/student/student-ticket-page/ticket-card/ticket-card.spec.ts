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
    };
    const fixture = TestBed.createComponent(TicketCard);
    fixture.componentRef.setInput('ticket', ticketWithoutQueuePosition);

    fixture.detectChanges();

    const cardElement = fixture.nativeElement as HTMLElement;
    expect(cardElement.querySelector('.ticket-status')?.textContent?.trim()).toBe('');
    expect(cardElement.querySelector('.state-chip')?.textContent?.trim()).toBe('Waiting');
  });
});
