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

describe('CatalogPanel for counters', () => {
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

  it('render_withCounter_showsServiceNameAsDetailText', () => {
    expect(panelElement.querySelector('.list-row-detail')?.textContent?.trim()).toBe(
      'Registration',
    );
  });

  it('render_withCounter_labelsPencilButtonWithEditAndName', () => {
    expect(panelElement.querySelector('.row-icon-button')?.getAttribute('aria-label')).toBe(
      'Edit Counter 1',
    );
  });

  it('openAdd_withNoActiveService_showsMessageAndKeepsDialogClosed', () => {
    clickAddCounter();
    flushNextServicesRequest([inactiveRegistration]);

    expect(panelElement.querySelector('.alert')?.textContent?.trim()).toBe(noActiveServiceMessage);
    expect(showModalSpy).not.toHaveBeenCalled();
  });

  it('openAdd_withOneActiveService_preselectsThatService', () => {
    clickAddCounter();
    flushNextServicesRequest([activeRegistration, { ...activeDocuments, active: false }]);

    const select = panelElement.querySelector<HTMLSelectElement>('dialog select');
    expect(select?.selectedOptions[0]?.textContent?.trim()).toBe('Registration');
  });

  it('openAdd_withTwoActiveServices_listsBothAndPreselectsNone', () => {
    clickAddCounter();
    flushNextServicesRequest([activeRegistration, activeDocuments]);

    expect(dialogOptionTexts()).toEqual(['Choose a service', 'Registration', 'Documents']);
    const select = panelElement.querySelector<HTMLSelectElement>('dialog select');
    expect(select?.selectedOptions[0]?.textContent?.trim()).toBe('Choose a service');
  });

  it('openAdd_calledTwiceBeforeServicesLoad_opensDialogOnce', () => {
    clickAddCounter();
    clickAddCounter();
    httpTesting
      .match(servicesUrl)
      .forEach((request) => request.flush([activeRegistration, activeDocuments]));

    expect(showModalSpy).toHaveBeenCalledTimes(1);
  });

  it('openEdit_withInactiveCurrentService_includesCurrentService', () => {
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

  it('openEdit_withInactiveOtherService_leavesOtherServiceOut', () => {
    clickEditCounterOne();
    flushNextServicesRequest([activeRegistration, { ...activeDocuments, active: false }]);

    expect(dialogOptionTexts()).toEqual(['Choose a service', 'Registration']);
  });

  it('submit_withCounterEdit_callsUpdateCounterWithServiceId', () => {
    clickEditCounterOne();
    flushNextServicesRequest([activeRegistration, activeDocuments]);

    submitDialogForm();

    const request = httpTesting.expectOne(`${countersUrl}/5`);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ name: 'Counter 1', serviceId: 1 });
    request.flush(counterOne);
    httpTesting.expectOne(countersUrl).flush([counterOne]);
  });

  it('submit_withNewCounterAndOneActiveService_postsNameAndServiceId', () => {
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

  it('submit_withServiceMissingOnServer_showsMessageAndRefreshesLists', () => {
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

  it('submit_withInactiveServiceRejected_showsServiceMessageAndRefreshesServices', () => {
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

describe('CatalogPanel for services', () => {
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

  it('render_withService_labelsPencilButtonWithRenameAndName', () => {
    expect(panelElement.querySelector('.row-icon-button')?.getAttribute('aria-label')).toBe(
      'Rename Registration',
    );
  });

  it('openEdit_withService_opensWithoutLoadingServicesAndHidesServiceSelect', () => {
    panelElement.querySelector<HTMLButtonElement>('.row-icon-button')?.click();
    fixture.detectChanges();

    expect(panelElement.querySelector('dialog h3')?.textContent?.trim()).toBe('Rename service');
    expect(panelElement.querySelector('dialog select')).toBeNull();
  });
});
