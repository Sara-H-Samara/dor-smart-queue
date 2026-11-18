import { Component, input, output } from '@angular/core';

import { AppIcon } from '../icon/app-icon';

@Component({
  selector: 'app-page-header',
  imports: [AppIcon],
  templateUrl: './page-header.html',
  styleUrl: './page-header.scss',
})
export class PageHeader {
  readonly contextText = input<string | null>(null);
  readonly secondaryActionLabel = input<string | null>(null);
  readonly showSignOut = input(false);
  readonly secondaryActionRequested = output<void>();
  readonly signOutRequested = output<void>();
}
