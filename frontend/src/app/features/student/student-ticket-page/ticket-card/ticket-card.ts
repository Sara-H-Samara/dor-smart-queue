import { Component, computed, input, output } from '@angular/core';

import { ActiveTicket } from '../../../../core/models/ticket.model';

@Component({
  selector: 'app-ticket-card',
  templateUrl: './ticket-card.html',
  styleUrl: './ticket-card.scss',
})
export class TicketCard {
  readonly ticket = input.required<ActiveTicket>();
  readonly isLeaving = input(false);
  readonly leaveQueueRequested = output<void>();

  protected readonly isCalled = computed(() => this.ticket().status === 'CALLED');
  protected readonly stateLabel = computed(() => (this.isCalled() ? 'Called' : 'Waiting'));

  protected readonly peopleAheadText = computed<string | null>(() => {
    const peopleAhead = this.ticket().peopleAhead;
    if (peopleAhead === null) {
      return null;
    }
    if (peopleAhead === 0) {
      return 'You are next';
    }
    return peopleAhead === 1 ? '1 person ahead of you' : `${peopleAhead} people ahead of you`;
  });

  protected readonly estimatedWaitText = computed<string | null>(() => {
    const estimatedWaitMinutes = this.ticket().estimatedWaitMinutes;
    return estimatedWaitMinutes === null ? null : `About ${estimatedWaitMinutes} min (estimate)`;
  });
}
