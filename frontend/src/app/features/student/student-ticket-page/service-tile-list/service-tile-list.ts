import { Component, input, output } from '@angular/core';

import { ActiveService } from '../../../../core/models/active-service.model';

@Component({
  selector: 'app-service-tile-list',
  templateUrl: './service-tile-list.html',
  styleUrl: './service-tile-list.scss',
})
export class ServiceTileList {
  readonly services = input.required<ActiveService[]>();
  readonly isTakingTicket = input(false);
  readonly serviceChosen = output<ActiveService>();
}
