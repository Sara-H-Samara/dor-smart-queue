import { Component, input } from '@angular/core';

import { WaitingTicket } from '../../../../core/models/staff-desk.model';

@Component({
  selector: 'app-waiting-ticket-list',
  templateUrl: './waiting-ticket-list.html',
  styleUrl: './waiting-ticket-list.scss',
})
export class WaitingTicketList {
  readonly tickets = input.required<WaitingTicket[]>();
  readonly serviceName = input.required<string>();
}
