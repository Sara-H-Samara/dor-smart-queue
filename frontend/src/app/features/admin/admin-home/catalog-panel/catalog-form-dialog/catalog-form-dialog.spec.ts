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

  it('render_withServiceHeading_labelsNameFieldWithJustName', () => {
    fixture.componentRef.setInput('idPrefix', 'services');
    fixture.componentRef.setInput('heading', 'Add service');

    fixture.detectChanges();

    expect(dialogElement.querySelector('label[for="services-name"]')?.textContent?.trim()).toBe(
      'Name',
    );
    expect(dialogElement.querySelector('h3')?.textContent?.trim()).toBe('Add service');
  });

  it('render_withServiceField_listsGivenServicesInSelect', () => {
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

  it('render_withoutServiceField_hidesSelect', () => {
    fixture.detectChanges();

    expect(dialogElement.querySelector('select')).toBeNull();
  });

  it('open_withInitialServiceId_selectsCurrentService', () => {
    fixture.componentRef.setInput('showServiceField', true);
    fixture.componentRef.setInput('serviceOptions', [registrationService, documentsService]);
    fixture.detectChanges();

    fixture.componentInstance.open({ name: 'Counter 1', serviceId: 2 });
    fixture.detectChanges();

    expect(selectedServiceText()).toBe('Documents (inactive)');
  });

  it('submit_withoutService_showsServiceErrorAndDoesNotEmit', () => {
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

  it('submit_withNameAndService_emitsNameAndServiceId', () => {
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
