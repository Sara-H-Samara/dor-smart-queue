import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NextTicketOnScreen } from '../../../../core/models/waiting-room.model';
import { NextTicketList } from './next-ticket-list';

const nextTickets: NextTicketOnScreen[] = [
  { ticketCode: 'R-015', serviceName: 'Registration' },
  { ticketCode: 'D-008', serviceName: 'Documents' },
];

describe('NextTicketList', () => {
  let fixture: ComponentFixture<NextTicketList>;

  function renderWith(tickets: NextTicketOnScreen[], moreWaitingCount: number): HTMLElement {
    fixture = TestBed.createComponent(NextTicketList);
    fixture.componentRef.setInput('nextTickets', tickets);
    fixture.componentRef.setInput('moreWaitingCount', moreWaitingCount);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  function textsOf(listElement: HTMLElement, selector: string): (string | undefined)[] {
    return Array.from(listElement.querySelectorAll(selector)).map((element) =>
      element.textContent?.trim(),
    );
  }

  it('shows each next ticket code with its service name', () => {
    const listElement = renderWith(nextTickets, 0);

    expect(textsOf(listElement, '.next-ticket-code')).toEqual(['R-015', 'D-008']);
    expect(textsOf(listElement, '.next-ticket-service')).toEqual(['Registration', 'Documents']);
  });

  it('shows how many more people are waiting', () => {
    const listElement = renderWith(nextTickets, 4);

    expect(textsOf(listElement, '.more-waiting')).toEqual(['4 more waiting']);
  });

  it('hides the more waiting line when nobody else is waiting', () => {
    const listElement = renderWith(nextTickets, 0);

    expect(listElement.querySelector('.more-waiting')).toBeNull();
  });

  it('shows a note instead of the list when no ticket is waiting', () => {
    const listElement = renderWith([], 0);

    expect(textsOf(listElement, '.no-one-waiting')).toEqual(['No one is waiting']);
    expect(listElement.querySelector('.next-tickets')).toBeNull();
  });
});
