import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CatalogRow } from './catalog-row';

describe('CatalogRow', () => {
  let fixture: ComponentFixture<CatalogRow>;
  let rowElement: HTMLElement;

  beforeEach(() => {
    fixture = TestBed.createComponent(CatalogRow);
    fixture.componentRef.setInput('item', { id: 1, name: 'Registration', code: 'R', active: true });
    fixture.componentRef.setInput('tileText', 'R');
    rowElement = fixture.nativeElement as HTMLElement;
  });

  it('clickSwitch_withActiveItem_emitsActiveChanged', () => {
    const emittedEvents: unknown[] = [];
    fixture.componentInstance.activeChanged.subscribe(() => emittedEvents.push(true));
    fixture.detectChanges();

    rowElement.querySelector<HTMLInputElement>('input[role="switch"]')?.click();

    expect(emittedEvents.length).toBe(1);
  });

  it('clickRename_withActiveItem_emitsRenameRequested', () => {
    const emittedEvents: unknown[] = [];
    fixture.componentInstance.renameRequested.subscribe(() => emittedEvents.push(true));
    fixture.detectChanges();

    rowElement.querySelector<HTMLButtonElement>('.rename-link')?.click();

    expect(emittedEvents.length).toBe(1);
  });

  it('render_whenBusy_disablesSwitchAndRename', () => {
    fixture.componentRef.setInput('isBusy', true);

    fixture.detectChanges();

    expect(rowElement.querySelector<HTMLInputElement>('input[role="switch"]')?.disabled).toBe(true);
    expect(rowElement.querySelector<HTMLButtonElement>('.rename-link')?.disabled).toBe(true);
  });

  it('render_withNullTileText_showsNoTile', () => {
    fixture.componentRef.setInput('tileText', null);

    fixture.detectChanges();

    expect(rowElement.querySelector('.notched-code-tile')).toBeNull();
  });

  it('render_withInactiveItem_showsInactiveAndUncheckedSwitch', () => {
    fixture.componentRef.setInput('item', { id: 1, name: 'Registration', active: false });

    fixture.detectChanges();

    expect(rowElement.querySelector('.toggle-state')?.textContent?.trim()).toBe('Inactive');
    expect(rowElement.querySelector<HTMLInputElement>('input[role="switch"]')?.checked).toBe(false);
  });
});
