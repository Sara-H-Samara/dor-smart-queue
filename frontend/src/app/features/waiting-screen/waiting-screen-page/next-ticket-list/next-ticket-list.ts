import { Component, input } from '@angular/core';

import { NextTicketOnScreen } from '../../../../core/models/waiting-room.model';

@Component({
  selector: 'app-next-ticket-list',
  templateUrl: './next-ticket-list.html',
  styleUrl: './next-ticket-list.scss',
})
export class NextTicketList {
  readonly nextTickets = input.required<NextTicketOnScreen[]>();
  readonly moreWaitingCount = input.required<number>();
}
