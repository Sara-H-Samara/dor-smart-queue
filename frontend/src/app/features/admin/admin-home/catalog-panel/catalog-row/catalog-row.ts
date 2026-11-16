import { Component, input, output } from '@angular/core';

import { AppIcon } from '../../../../../shared/icon/app-icon';
import { CatalogItem } from '../../../../../core/models/catalog.model';

@Component({
  selector: 'app-catalog-row',
  imports: [AppIcon],
  templateUrl: './catalog-row.html',
  styleUrl: './catalog-row.scss',
})
export class CatalogRow {
  readonly item = input.required<CatalogItem>();
  readonly tileText = input.required<string | null>();
  readonly detailText = input<string | null>(null);
  readonly isBusy = input(false);
  readonly editVerb = input('Edit');

  readonly activeChanged = output<void>();
  readonly editRequested = output<void>();

  protected onSwitchChanged(event: Event): void {
    const switchInput = event.target as HTMLInputElement;
    switchInput.checked = this.item().active;
    this.activeChanged.emit();
  }
}
