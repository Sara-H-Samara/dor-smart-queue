import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { environment } from '../../../../../environments/environment';
import { CatalogItem } from '../../../../core/models/catalog.model';
import { installDialogSupport } from '../../../../testing/dialog-support';
import { noActiveServiceMessage } from './catalog-errors';
import { CatalogPanel } from './catalog-panel';

const countersUrl = `${environment.apiUrl}/api/admin/counters`;
const servicesUrl = `${environment.apiUrl}/api/admin/services`;
const counterOne: CatalogItem = {
  id: 5,
  name: 'Counter 1',
  active: true,
  serviceId: 1,
  serviceName: 'Registration',
};
const activeRegistration: CatalogItem = { id: 1, name: 'Registration', code: 'R', active: true };
const inactiveRegistration: CatalogItem = { ...activeRegistration, active: false };
const activeDocuments: CatalogItem = { id: 2, name: 'Documents', code: 'D', active: true };

describe('CatalogPanel with counters', () => {
  let fixture: ComponentFixture<CatalogPanel>;
  let panelElement: HTMLElement;
  let httpTesting: HttpTestingController;
  let showModalSpy: ReturnType<typeof vi.spyOn>;

  beforeAll(() => installDialogSupport());

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpTesting = TestBed.inject(HttpTestingController);
    showModalSpy = vi.spyOn(HTMLDialogElement.prototype, 'showModal');
    fixture = TestBed.createComponent(CatalogPanel);
    fixture.componentRef.setInput('catalogResource', 'counters');
    fixture.componentRef.setInput('heading', 'Counters');
    fixture.componentRef.setInput('itemLabel', 'Counter');
    fixture.componentRef.setInput(
      'emptyText',
      'Add a counter so staff can start serving students.',
    );
    panelElement = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    httpTesting.expectOne(countersUrl).flush([counterOne]);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTesting.verify();
    vi.restoreAllMocks();
  });

  function flushNextServicesRequest(services: CatalogItem[]): void {
    httpTesting.expectOne(servicesUrl).flush(services);
    fixture.detectChanges();
  }

  function clickAddCounter(): void {
    panelElement.querySelector<HTMLButtonElement>('.list-add-row')?.click();
  }

  function clickEditCounterOne(): void {
    panelElement.querySelector<HTMLButtonElement>('.row-icon-button')?.click();
  }

  function submitDialogForm(): void {
    panelElement.querySelector('form')?.dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();
  }

  function dialogOptionTexts(): (string | undefined)[] {
    return Array.from(panelElement.querySelectorAll('dialog select option')).map((option) =>
      option.textContent?.trim(),
    );
  }

  it('shows the service name as detail text for a counter', () => {
    expect(panelElement.querySelector('.list-row-detail')?.textContent?.trim()).toBe(
      'Registration',
    );
  });

  it('labels the pencil button with Edit and the counter name', () => {
    expect(panelElement.querySelector('.row-icon-button')?.getAttribute('aria-label')).toBe(
      'Edit Counter 1',
    );
  });

  it('shows a message and keeps the dialog closed when no service is active', () => {
    clickAddCounter();
    flushNextServicesRequest([inactiveRegistration]);

    expect(panelElement.querySelector('.alert')?.textContent?.trim()).toBe(noActiveServiceMessage);
    expect(showModalSpy).not.toHaveBeenCalled();
  });

  it('preselects the only active service when adding a counter', () => {
    clickAddCounter();
    flushNextServicesRequest([activeRegistration, { ...activeDocuments, active: false }]);

    const select = panelElement.querySelector<HTMLSelectElement>('dialog select');
    expect(select?.selectedOptions[0]?.textContent?.trim()).toBe('Registration');
  });

  it('lists both active services and preselects none when adding a counter', () => {
    clickAddCounter();
    flushNextServicesRequest([activeRegistration, activeDocuments]);

    expect(dialogOptionTexts()).toEqual(['Choose a service', 'Registration', 'Documents']);
    const select = panelElement.querySelector<HTMLSelectElement>('dialog select');
    expect(select?.selectedOptions[0]?.textContent?.trim()).toBe('Choose a service');
  });

  it('opens the dialog once when add is clicked twice before the services load', () => {
    clickAddCounter();
    clickAddCounter();
    httpTesting
      .match(servicesUrl)
      .forEach((request) => request.flush([activeRegistration, activeDocuments]));

    expect(showModalSpy).toHaveBeenCalledTimes(1);
  });

  it('includes an inactive current service when editing a counter', () => {
    clickEditCounterOne();
    flushNextServicesRequest([inactiveRegistration, activeDocuments]);

    expect(dialogOptionTexts()).toEqual([
      'Choose a service',
      'Registration (inactive)',
      'Documents',
    ]);
    const select = panelElement.querySelector<HTMLSelectElement>('dialog select');
    expect(select?.selectedOptions[0]?.textContent?.trim()).toBe('Registration (inactive)');
  });

  it('leaves out other inactive services when editing a counter', () => {
    clickEditCounterOne();
    flushNextServicesRequest([activeRegistration, { ...activeDocuments, active: false }]);

    expect(dialogOptionTexts()).toEqual(['Choose a service', 'Registration']);
  });

  it('sends a PUT request with the chosen service id when a counter is edited', () => {
    clickEditCounterOne();
    flushNextServicesRequest([activeRegistration, activeDocuments]);

    submitDialogForm();

    const request = httpTesting.expectOne(`${countersUrl}/5`);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ name: 'Counter 1', serviceId: 1 });
    request.flush(counterOne);
    httpTesting.expectOne(countersUrl).flush([counterOne]);
  });

  it('sends a POST request with the name and service id for a new counter', () => {
    clickAddCounter();
    flushNextServicesRequest([activeRegistration]);
    const nameInput = panelElement.querySelector<HTMLInputElement>('dialog input');
    nameInput!.value = 'Counter 2';
    nameInput!.dispatchEvent(new Event('input'));

    submitDialogForm();

    const request = httpTesting.expectOne(countersUrl);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ name: 'Counter 2', serviceId: 1 });
    request.flush(counterOne);
    httpTesting.expectOne(countersUrl).flush([counterOne]);
  });

  it('shows a message and refreshes the lists when the service is missing on the server', () => {
    clickEditCounterOne();
    flushNextServicesRequest([activeRegistration, activeDocuments]);
    submitDialogForm();

    httpTesting
      .expectOne(`${countersUrl}/5`)
      .flush('Service not found.', { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();

    expect(panelElement.querySelector('dialog .alert')?.textContent?.trim()).toBe(
      'This counter or its service was not found. It may have been removed; the list is refreshed.',
    );
    httpTesting.expectOne(countersUrl).flush([counterOne]);
    httpTesting.expectOne(servicesUrl).flush([activeDocuments]);
  });

  it('shows the service message and refreshes the services when the server rejects it', () => {
    clickEditCounterOne();
    flushNextServicesRequest([activeRegistration, activeDocuments]);
    submitDialogForm();

    httpTesting
      .expectOne(`${countersUrl}/5`)
      .flush('This service is not available.', { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(panelElement.querySelector('dialog .alert')?.textContent?.trim()).toBe(
      'Choose an active service and a name of 1 to 100 characters.',
    );
    httpTesting.expectOne(servicesUrl).flush([inactiveRegistration, activeDocuments]);
  });
});

describe('CatalogPanel with services', () => {
  let fixture: ComponentFixture<CatalogPanel>;
  let panelElement: HTMLElement;
  let httpTesting: HttpTestingController;

  beforeAll(() => installDialogSupport());

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpTesting = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CatalogPanel);
    fixture.componentRef.setInput('catalogResource', 'services');
    fixture.componentRef.setInput('heading', 'Services');
    fixture.componentRef.setInput('itemLabel', 'Service');
    fixture.componentRef.setInput(
      'emptyText',
      'Add a service so students can start taking tickets.',
    );
    panelElement = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    httpTesting.expectOne(servicesUrl).flush([activeRegistration]);
    fixture.detectChanges();
  });

  afterEach(() => httpTesting.verify());

  it('labels the pencil button with Rename and the service name', () => {
    expect(panelElement.querySelector('.row-icon-button')?.getAttribute('aria-label')).toBe(
      'Rename Registration',
    );
  });

  it('opens without loading services and hides the service select', () => {
    panelElement.querySelector<HTMLButtonElement>('.row-icon-button')?.click();
    fixture.detectChanges();

    expect(panelElement.querySelector('dialog h3')?.textContent?.trim()).toBe('Rename service');
    expect(panelElement.querySelector('dialog select')).toBeNull();
  });
});
