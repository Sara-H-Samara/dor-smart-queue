import { Component, input } from '@angular/core';

import { CounterOnScreen } from '../../../../core/models/waiting-room.model';

@Component({
  selector: 'app-counter-tile',
  templateUrl: './counter-tile.html',
  styleUrl: './counter-tile.scss',
})
export class CounterTile {
  readonly counter = input.required<CounterOnScreen>();
}
