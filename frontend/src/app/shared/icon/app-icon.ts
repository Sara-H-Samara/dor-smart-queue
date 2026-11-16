import { Component, computed, input } from '@angular/core';

import { IconName } from './icon-name';
import { ICON_PATHS } from './icon-paths';

@Component({
  selector: 'app-icon',
  templateUrl: './app-icon.html',
  styleUrl: './app-icon.scss',
  host: { 'aria-hidden': 'true' },
})
export class AppIcon {
  readonly iconName = input.required<IconName>();

  protected readonly iconPath = computed(() => ICON_PATHS[this.iconName()]);
}
