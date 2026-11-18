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

  it('emits an active change when the switch is clicked', () => {
    const emittedEvents: unknown[] = [];
    fixture.componentInstance.activeChanged.subscribe(() => emittedEvents.push(true));
    fixture.detectChanges();

    rowElement.querySelector<HTMLInputElement>('input[role="switch"]')?.click();

    expect(emittedEvents.length).toBe(1);
  });

  it('emits an edit request when the edit button is clicked', () => {
    const emittedEvents: unknown[] = [];
    fixture.componentInstance.editRequested.subscribe(() => emittedEvents.push(true));
    fixture.detectChanges();

    rowElement.querySelector<HTMLButtonElement>('.row-icon-button')?.click();

    expect(emittedEvents.length).toBe(1);
  });

  it('disables the switch and the edit button while busy', () => {
    fixture.componentRef.setInput('isBusy', true);

    fixture.detectChanges();

    expect(rowElement.querySelector<HTMLInputElement>('input[role="switch"]')?.disabled).toBe(true);
    expect(rowElement.querySelector<HTMLButtonElement>('.row-icon-button')?.disabled).toBe(true);
  });

  it('shows the desk icon instead of text when there is no tile text', () => {
    fixture.componentRef.setInput('tileText', null);

    fixture.detectChanges();

    expect(rowElement.querySelector('.notched-code-tile app-icon')).not.toBeNull();
    expect(rowElement.querySelector('.notched-code-tile')?.textContent?.trim()).toBe('');
  });

  it('labels the edit button with Edit and the item name', () => {
    fixture.detectChanges();

    expect(rowElement.querySelector('.row-icon-button')?.getAttribute('aria-label')).toBe(
      'Edit Registration',
    );
  });

  it('labels the edit button with Rename and the item name', () => {
    fixture.componentRef.setInput('editVerb', 'Rename');

    fixture.detectChanges();

    expect(rowElement.querySelector('.row-icon-button')?.getAttribute('aria-label')).toBe(
      'Rename Registration',
    );
  });

  it('shows Inactive and an unchecked switch for an inactive item', () => {
    fixture.componentRef.setInput('item', { id: 1, name: 'Registration', active: false });

    fixture.detectChanges();

    expect(rowElement.querySelector('.toggle-state')?.textContent?.trim()).toBe('Inactive');
    expect(rowElement.querySelector<HTMLInputElement>('input[role="switch"]')?.checked).toBe(false);
  });
});
