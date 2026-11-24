import { Component, computed, input, output } from '@angular/core';

import { AppIcon } from '../../../../shared/icon/app-icon';
import { ActiveTicket } from '../../../../core/models/ticket.model';
import { alertPromiseTextFor } from './alert-promise-text';

@Component({
  selector: 'app-ticket-card',
  imports: [AppIcon],
  templateUrl: './ticket-card.html',
  styleUrl: './ticket-card.scss',
})
export class TicketCard {
  readonly ticket = input.required<ActiveTicket>();
  readonly isLeaving = input(false);
  readonly leaveQueueRequested = output<void>();

  protected readonly isCalled = computed(() => this.ticket().status === 'CALLED');
  protected readonly stateLabel = computed(() => (this.isCalled() ? 'Called' : 'Waiting'));
  protected readonly hasNoOpenCounter = computed(
    () => this.ticket().status === 'WAITING' && !this.ticket().hasOpenCounter,
  );

  protected readonly calledHeadingText = computed(() => {
    const counterName = this.ticket().counterName;
    return counterName ? `Your turn: ${counterName}` : 'Your turn';
  });

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

  protected readonly turnNearText = computed<string | null>(() =>
    this.ticket().status === 'WAITING' && this.ticket().turnIsNear
      ? 'Your turn is near. Please come to the counters.'
      : null,
  );

  protected readonly alertPromiseText = computed<string | null>(() => {
    const alertAtPeopleAhead = this.ticket().alertAtPeopleAhead;
    if (this.ticket().turnIsNear || alertAtPeopleAhead === null) {
      return null;
    }
    return alertPromiseTextFor(alertAtPeopleAhead);
  });

  protected readonly estimatedWaitText = computed<string | null>(() => {
    const estimatedWaitMinutes = this.ticket().estimatedWaitMinutes;
    return estimatedWaitMinutes === null ? null : `About ${estimatedWaitMinutes} min`;
  });
}
