import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CatalogItem } from '../../../../../core/models/catalog.model';
import { installDialogSupport } from '../../../../../testing/dialog-support';
import { CatalogFormDialog, CatalogFormValue } from './catalog-form-dialog';

const registrationService: CatalogItem = { id: 1, name: 'Registration', code: 'R', active: true };
const documentsService: CatalogItem = { id: 2, name: 'Documents', code: 'D', active: false };

describe('CatalogFormDialog', () => {
  let fixture: ComponentFixture<CatalogFormDialog>;
  let dialogElement: HTMLElement;

  beforeAll(() => installDialogSupport());

  beforeEach(() => {
    fixture = TestBed.createComponent(CatalogFormDialog);
    fixture.componentRef.setInput('idPrefix', 'counters');
    fixture.componentRef.setInput('heading', 'Add counter');
    dialogElement = fixture.nativeElement as HTMLElement;
  });

  function submitForm(): void {
    dialogElement.querySelector('form')?.dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();
  }

  function selectedServiceText(): string | undefined {
    const select = dialogElement.querySelector<HTMLSelectElement>('select');
    return select?.selectedOptions[0]?.textContent?.trim();
  }

  it('shows the given heading and labels the name field Name', () => {
    fixture.componentRef.setInput('idPrefix', 'services');
    fixture.componentRef.setInput('heading', 'Add service');

    fixture.detectChanges();

    expect(dialogElement.querySelector('label[for="services-name"]')?.textContent?.trim()).toBe(
      'Name',
    );
    expect(dialogElement.querySelector('h3')?.textContent?.trim()).toBe('Add service');
  });

  it('lists the given services in the select and marks inactive ones', () => {
    fixture.componentRef.setInput('showServiceField', true);
    fixture.componentRef.setInput('serviceOptions', [registrationService, documentsService]);

    fixture.detectChanges();

    const optionTexts = Array.from(dialogElement.querySelectorAll('select option')).map((option) =>
      option.textContent?.trim(),
    );
    expect(optionTexts).toEqual(['Choose a service', 'Registration', 'Documents (inactive)']);
    expect(dialogElement.querySelector('label[for="counters-service"]')?.textContent?.trim()).toBe(
      'Service',
    );
  });

  it('hides the select when the form has no service field', () => {
    fixture.detectChanges();

    expect(dialogElement.querySelector('select')).toBeNull();
  });

  it('selects the initial service when the form opens', () => {
    fixture.componentRef.setInput('showServiceField', true);
    fixture.componentRef.setInput('serviceOptions', [registrationService, documentsService]);
    fixture.detectChanges();

    fixture.componentInstance.open({ name: 'Counter 1', serviceId: 2 });
    fixture.detectChanges();

    expect(selectedServiceText()).toBe('Documents (inactive)');
  });

  it('shows a service error and emits nothing when no service is chosen', () => {
    const emittedValues: CatalogFormValue[] = [];
    fixture.componentInstance.submitted.subscribe((value) => emittedValues.push(value));
    fixture.componentRef.setInput('showServiceField', true);
    fixture.componentRef.setInput('serviceOptions', [registrationService]);
    fixture.detectChanges();
    fixture.componentInstance.open({ name: 'Counter 1' });

    submitForm();

    expect(emittedValues).toEqual([]);
    expect(dialogElement.querySelector('#counters-service-error')?.textContent?.trim()).toBe(
      'Choose a service.',
    );
  });

  it('emits the name and service id when both are filled in', () => {
    const emittedValues: CatalogFormValue[] = [];
    fixture.componentInstance.submitted.subscribe((value) => emittedValues.push(value));
    fixture.componentRef.setInput('showServiceField', true);
    fixture.componentRef.setInput('serviceOptions', [registrationService, documentsService]);
    fixture.detectChanges();
    fixture.componentInstance.open({ name: 'Counter 1', serviceId: 1 });

    submitForm();

    expect(emittedValues).toEqual([{ name: 'Counter 1', serviceId: 1 }]);
  });
});
