import { Component, input, output } from '@angular/core';

import { TicketDistance } from '../../../../core/models/ticket.model';
import { DISTANCE_OPTIONS } from './distance-options';

@Component({
  selector: 'app-distance-picker',
  templateUrl: './distance-picker.html',
  styleUrl: './distance-picker.scss',
})
export class DistancePicker {
  readonly selectedDistance = input.required<TicketDistance>();
  readonly isDisabled = input(false);
  readonly distanceChosen = output<TicketDistance>();

  protected readonly distanceOptions = DISTANCE_OPTIONS;
}
