import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-call-next-panel',
  templateUrl: './call-next-panel.html',
  styleUrl: './call-next-panel.scss',
})
export class CallNextPanel {
  readonly canCallNext = input.required<boolean>();
  readonly isCalling = input(false);
  readonly notice = input<string | null>(null);
  readonly callNextRequested = output<void>();
}
