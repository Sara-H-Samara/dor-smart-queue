import { TestBed } from '@angular/core/testing';

import { AppIcon } from './app-icon';
import { IconName } from './icon-name';
import { ICON_PATHS } from './icon-paths';

describe('AppIcon', () => {
  function renderIcon(iconName: IconName): HTMLElement {
    const fixture = TestBed.createComponent(AppIcon);
    fixture.componentRef.setInput('iconName', iconName);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('render_withIconName_drawsThatIconPath', () => {
    const iconElement = renderIcon('plus');

    expect(iconElement.querySelector('path')?.getAttribute('d')).toBe(ICON_PATHS.plus);
  });

  it('render_always_hidesSvgFromScreenReaders', () => {
    const iconElement = renderIcon('pencil');

    expect(iconElement.getAttribute('aria-hidden')).toBe('true');
    expect(iconElement.querySelector('svg')?.getAttribute('aria-hidden')).toBe('true');
  });

  it('render_withEachIconName_drawsNonEmptyPath', () => {
    const iconNames = Object.keys(ICON_PATHS) as IconName[];

    for (const iconName of iconNames) {
      expect(renderIcon(iconName).querySelector('path')?.getAttribute('d')?.length).toBeGreaterThan(
        0,
      );
    }
  });
});
