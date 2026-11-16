import { TestBed } from '@angular/core/testing';

import { CatalogFormDialog } from './catalog-form-dialog';

describe('CatalogFormDialog', () => {
  it('render_withServiceHeading_labelsNameFieldWithJustName', () => {
    const fixture = TestBed.createComponent(CatalogFormDialog);
    fixture.componentRef.setInput('idPrefix', 'services');
    fixture.componentRef.setInput('heading', 'Add service');

    fixture.detectChanges();

    const dialogElement = fixture.nativeElement as HTMLElement;
    expect(dialogElement.querySelector('label[for="services-name"]')?.textContent?.trim()).toBe(
      'Name',
    );
    expect(dialogElement.querySelector('h3')?.textContent?.trim()).toBe('Add service');
  });
});
