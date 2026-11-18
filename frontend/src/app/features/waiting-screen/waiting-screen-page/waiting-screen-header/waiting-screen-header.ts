import { Component, input } from '@angular/core';

@Component({
  selector: 'app-waiting-screen-header',
  templateUrl: './waiting-screen-header.html',
  styleUrl: './waiting-screen-header.scss',
})
export class WaitingScreenHeader {
  readonly organizationName = input.required<string>();
}
