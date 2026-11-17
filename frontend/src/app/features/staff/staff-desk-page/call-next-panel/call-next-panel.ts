import { Component, computed, input, output } from '@angular/core';

@Component({
  selector: 'app-call-next-panel',
  templateUrl: './call-next-panel.html',
  styleUrl: './call-next-panel.scss',
})
export class CallNextPanel {
  readonly canCallNext = input.required<boolean>();
  readonly isCalling = input(false);
  readonly waitingCount = input<number | null>(null);
  readonly serviceName = input('');
  readonly callNextRequested = output<void>();

  protected readonly waitingCountText = computed<string | null>(() => {
    const waitingCount = this.waitingCount();
    return waitingCount === null ? null : `${waitingCount} waiting for ${this.serviceName()}`;
  });
}
