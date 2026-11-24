import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { environment } from '../../../../../environments/environment';
import { CatalogItem } from '../../../../core/models/catalog.model';
import { installDialogSupport } from '../../../../testing/dialog-support';
import { FakeLiveUpdateWatch } from '../../../../testing/fake-live-update-watch';
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

function submitFormOf(fixture: ComponentFixture<CatalogPanel>): void {
  (fixture.nativeElement as HTMLElement)
    .querySelector('form')
    ?.dispatchEvent(new Event('submit', { cancelable: true }));
  fixture.detectChanges();
}

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
    const liveUpdateWatch = new FakeLiveUpdateWatch();
    fixture.componentRef.setInput('catalogResource', 'counters');
    fixture.componentRef.setInput('firstLoadRequest', liveUpdateWatch.firstLoadRequest);
    fixture.componentRef.setInput('refreshRequests', liveUpdateWatch.refreshRequests);
    fixture.componentRef.setInput('heading', 'Counters');
    fixture.componentRef.setInput('itemLabel', 'Counter');
    fixture.componentRef.setInput(
      'emptyText',
      'Add a counter so staff can start serving students.',
    );
    panelElement = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    liveUpdateWatch.requestFirstLoad();
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
    submitFormOf(fixture);
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
    const liveUpdateWatch = new FakeLiveUpdateWatch();
    fixture.componentRef.setInput('catalogResource', 'services');
    fixture.componentRef.setInput('firstLoadRequest', liveUpdateWatch.firstLoadRequest);
    fixture.componentRef.setInput('refreshRequests', liveUpdateWatch.refreshRequests);
    fixture.componentRef.setInput('heading', 'Services');
    fixture.componentRef.setInput('itemLabel', 'Service');
    fixture.componentRef.setInput(
      'emptyText',
      'Add a service so students can start taking tickets.',
    );
    panelElement = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    liveUpdateWatch.requestFirstLoad();
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

describe('CatalogPanel live refresh', () => {
  let fixture: ComponentFixture<CatalogPanel>;
  let panelElement: HTMLElement;
  let httpTesting: HttpTestingController;
  let refreshWatch: FakeLiveUpdateWatch;

  beforeAll(() => installDialogSupport());

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpTesting = TestBed.inject(HttpTestingController);
    refreshWatch = new FakeLiveUpdateWatch();
    fixture = TestBed.createComponent(CatalogPanel);
    fixture.componentRef.setInput('catalogResource', 'services');
    fixture.componentRef.setInput('firstLoadRequest', refreshWatch.firstLoadRequest);
    fixture.componentRef.setInput('refreshRequests', refreshWatch.refreshRequests);
    fixture.componentRef.setInput('heading', 'Services');
    fixture.componentRef.setInput('itemLabel', 'Service');
    fixture.componentRef.setInput(
      'emptyText',
      'Add a service so students can start taking tickets.',
    );
    panelElement = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    refreshWatch.requestFirstLoad();
    httpTesting.expectOne(servicesUrl).flush([activeRegistration]);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTesting.verify();
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  function cancelDialog(): void {
    panelElement.querySelector<HTMLButtonElement>('dialog .btn-quiet')?.click();
    fixture.detectChanges();
  }

  function requestRefresh(): void {
    refreshWatch.requestRefresh();
    fixture.detectChanges();
  }

  function itemCountText(): string | undefined {
    return panelElement.querySelector('.list-group-footer')?.textContent?.trim();
  }

  function clickActiveSwitch(): void {
    panelElement.querySelector<HTMLInputElement>('input[role="switch"]')?.click();
    fixture.detectChanges();
  }

  function clickEditButton(): void {
    panelElement.querySelector<HTMLButtonElement>('.row-icon-button')?.click();
    fixture.detectChanges();
  }

  it('requests the list again and shows the new items after a refresh request', () => {
    requestRefresh();

    httpTesting.expectOne(servicesUrl).flush([activeRegistration, activeDocuments]);
    fixture.detectChanges();

    expect(itemCountText()).toBe('2 services');
  });

  it('keeps showing the list while it refreshes', () => {
    requestRefresh();

    expect(panelElement.querySelector('[role="status"]')).toBeNull();
    expect(itemCountText()).toBe('1 service');
    httpTesting.expectOne(servicesUrl).flush([activeRegistration]);
  });

  it('uses only the newest answer when refreshes are requested close together', () => {
    requestRefresh();
    requestRefresh();

    const [olderRequest, newerRequest] = httpTesting.match(servicesUrl);
    newerRequest.flush([activeRegistration, activeDocuments]);
    fixture.detectChanges();

    expect(olderRequest.cancelled).toBe(true);
    expect(itemCountText()).toBe('2 services');
  });

  it('keeps the list and shows no alert when a refresh fails', () => {
    requestRefresh();

    httpTesting
      .expectOne(servicesUrl)
      .flush('Server error.', { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(itemCountText()).toBe('1 service');
    expect(panelElement.querySelector('.alert')).toBeNull();
  });

  it('refreshes again at the next request after a failed refresh', () => {
    requestRefresh();
    httpTesting
      .expectOne(servicesUrl)
      .flush('Server error.', { status: 500, statusText: 'Server Error' });

    requestRefresh();
    httpTesting.expectOne(servicesUrl).flush([activeRegistration, activeDocuments]);
    fixture.detectChanges();

    expect(itemCountText()).toBe('2 services');
  });

  it('sends no request while a change is being saved', () => {
    clickActiveSwitch();
    const saveRequest = httpTesting.expectOne(`${servicesUrl}/1/active`);

    requestRefresh();

    httpTesting.expectNone(servicesUrl);
    saveRequest.flush({ ...activeRegistration, active: false });
    httpTesting.expectOne(servicesUrl).flush([inactiveRegistration]);
  });

  it('sends no request while the dialog is open', () => {
    clickEditButton();

    requestRefresh();

    httpTesting.expectNone(servicesUrl);
  });

  it('refreshes again after the dialog is closed', () => {
    clickEditButton();
    panelElement.querySelector('dialog')?.close();
    fixture.detectChanges();

    requestRefresh();

    httpTesting.expectOne(servicesUrl).flush([activeRegistration]);
  });

  it('refreshes once when the dialog is cancelled after a refresh request was dropped', () => {
    clickEditButton();
    requestRefresh();
    httpTesting.expectNone(servicesUrl);

    cancelDialog();

    httpTesting.expectOne(servicesUrl).flush([activeRegistration, activeDocuments]);
    fixture.detectChanges();
    expect(itemCountText()).toBe('2 services');
  });

  it('refreshes only once when several refresh requests were dropped before the cancel', () => {
    clickEditButton();
    requestRefresh();
    requestRefresh();
    requestRefresh();

    cancelDialog();

    httpTesting.expectOne(servicesUrl).flush([activeRegistration]);
  });

  it('does not refresh when the dialog is cancelled and no refresh request was dropped', () => {
    clickEditButton();

    cancelDialog();

    httpTesting.expectNone(servicesUrl);
  });

  it('does not refresh a second time when the dialog is cancelled again later', () => {
    clickEditButton();
    requestRefresh();
    cancelDialog();
    httpTesting.expectOne(servicesUrl).flush([activeRegistration]);
    clickEditButton();

    cancelDialog();

    httpTesting.expectNone(servicesUrl);
  });

  it('refreshes when the dialog is cancelled after a failed save', () => {
    clickEditButton();
    requestRefresh();
    submitFormOf(fixture);
    httpTesting
      .expectOne(`${servicesUrl}/1`)
      .flush('Server error.', { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    cancelDialog();

    httpTesting.expectOne(servicesUrl).flush([activeRegistration]);
  });

  it('reloads only once after a save when a refresh request was dropped while the dialog was open', () => {
    clickEditButton();
    requestRefresh();

    submitFormOf(fixture);
    httpTesting.expectOne(`${servicesUrl}/1`).flush(activeRegistration);

    httpTesting.expectOne(servicesUrl).flush([activeRegistration]);
  });

  it('reloads only once after a save when the dialog close event arrives after the reload started', () => {
    vi.useFakeTimers();
    vi.spyOn(HTMLDialogElement.prototype, 'close').mockImplementation(function (
      this: HTMLDialogElement,
    ) {
      this.removeAttribute('open');
      setTimeout(() => this.dispatchEvent(new Event('close')));
    });
    clickEditButton();
    requestRefresh();
    submitFormOf(fixture);
    httpTesting.expectOne(`${servicesUrl}/1`).flush(activeRegistration);
    const reloadRequest = httpTesting.expectOne(servicesUrl);

    vi.runAllTimers();

    httpTesting.expectNone(servicesUrl);
    reloadRequest.flush([activeRegistration]);
  });

  it('ignores a late refresh answer after a save reloaded the list', () => {
    requestRefresh();
    const lateRefreshRequest = httpTesting.expectOne(servicesUrl);
    clickActiveSwitch();
    httpTesting
      .expectOne(`${servicesUrl}/1/active`)
      .flush({ ...activeRegistration, active: false });

    httpTesting.expectOne(servicesUrl).flush([inactiveRegistration]);
    fixture.detectChanges();

    expect(lateRefreshRequest.cancelled).toBe(true);
    expect(panelElement.querySelector('.toggle-state')?.textContent?.trim()).toBe('Inactive');
  });

  it('sends no request after the panel is destroyed', () => {
    fixture.destroy();

    refreshWatch.requestRefresh();

    httpTesting.expectNone(servicesUrl);
  });
});

describe('CatalogPanel first load', () => {
  let fixture: ComponentFixture<CatalogPanel>;
  let httpTesting: HttpTestingController;
  let liveUpdateWatch: FakeLiveUpdateWatch;

  beforeAll(() => installDialogSupport());

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpTesting = TestBed.inject(HttpTestingController);
    liveUpdateWatch = new FakeLiveUpdateWatch();
    fixture = TestBed.createComponent(CatalogPanel);
    fixture.componentRef.setInput('catalogResource', 'services');
    fixture.componentRef.setInput('firstLoadRequest', liveUpdateWatch.firstLoadRequest);
    fixture.componentRef.setInput('refreshRequests', liveUpdateWatch.refreshRequests);
    fixture.componentRef.setInput('heading', 'Services');
    fixture.componentRef.setInput('itemLabel', 'Service');
    fixture.componentRef.setInput(
      'emptyText',
      'Add a service so students can start taking tickets.',
    );
    fixture.detectChanges();
  });

  afterEach(() => httpTesting.verify());

  it('constructor_beforeFirstLoadRequest_sendsNoRequest', () => {
    httpTesting.expectNone(servicesUrl);
  });

  it('firstLoadRequest_whenEmitted_loadsTheListOnce', () => {
    liveUpdateWatch.requestFirstLoad();

    httpTesting.expectOne(servicesUrl).flush([activeRegistration]);
    fixture.detectChanges();

    expect(
      (fixture.nativeElement as HTMLElement)
        .querySelector('.list-group-footer')
        ?.textContent?.trim(),
    ).toBe('1 service');
  });
});
