import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { CatalogService } from './catalog.service';

describe('CatalogService', () => {
  let catalogService: CatalogService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    catalogService = TestBed.inject(CatalogService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('sends a POST request with the name and service id when creating a counter', () => {
    catalogService.createCounter('Counter 3', 7).subscribe();

    const request = httpTesting.expectOne(`${environment.apiUrl}/api/admin/counters`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ name: 'Counter 3', serviceId: 7 });
  });

  it('sends a PUT request with the name and service id when updating a counter', () => {
    catalogService.updateCounter(4, 'Counter 4', 7).subscribe();

    const request = httpTesting.expectOne(`${environment.apiUrl}/api/admin/counters/4`);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ name: 'Counter 4', serviceId: 7 });
  });
});
