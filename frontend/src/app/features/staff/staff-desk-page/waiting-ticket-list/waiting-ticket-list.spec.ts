import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WaitingTicket } from '../../../../core/models/staff-desk.model';
import { WaitingTicketList } from './waiting-ticket-list';

const waitingTickets: WaitingTicket[] = [
  { ticketCode: 'R-015', createdAt: '2026-11-16T09:31:00' },
  { ticketCode: 'R-016', createdAt: '2026-11-16T09:32:00' },
];

describe('WaitingTicketList', () => {
  let fixture: ComponentFixture<WaitingTicketList>;
  let listElement: HTMLElement;

  beforeEach(() => {
    fixture = TestBed.createComponent(WaitingTicketList);
    fixture.componentRef.setInput('serviceName', 'Course registration');
    listElement = fixture.nativeElement as HTMLElement;
  });

  it('shows the heading and one row per ticket', () => {
    fixture.componentRef.setInput('tickets', waitingTickets);

    fixture.detectChanges();

    expect(listElement.querySelector('.waiting-heading')?.textContent?.trim()).toBe('Waiting');
    expect(listElement.querySelectorAll('.waiting-row')).toHaveLength(2);
  });

  it('shows the code, service name and waiting chip in each row', () => {
    fixture.componentRef.setInput('tickets', waitingTickets);

    fixture.detectChanges();

    const rows = listElement.querySelectorAll('.waiting-row');
    expect(rows[0].querySelector('.waiting-code')?.textContent?.trim()).toBe('R-015');
    expect(rows[1].querySelector('.waiting-code')?.textContent?.trim()).toBe('R-016');
    rows.forEach((row) => {
      expect(row.querySelector('.list-row-name')?.textContent?.trim()).toBe('Course registration');
      expect(row.querySelector('.state-chip')?.textContent?.trim()).toBe('Waiting');
    });
  });

  it('shows no heading and no list when nobody is waiting', () => {
    fixture.componentRef.setInput('tickets', []);

    fixture.detectChanges();

    expect(listElement.querySelector('.waiting-heading')).toBeNull();
    expect(listElement.querySelector('.waiting-list')).toBeNull();
  });
});
