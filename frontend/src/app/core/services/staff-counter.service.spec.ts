import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { AvailableCounter } from '../models/staff-desk.model';
import { StaffCounterService } from './staff-counter.service';

describe('StaffCounterService', () => {
  let staffCounterService: StaffCounterService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    staffCounterService = TestBed.inject(StaffCounterService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('sends a GET request and returns the available counters', () => {
    const counters: AvailableCounter[] = [
      { id: 2, name: 'Counter 2', serviceId: 5, serviceName: 'Course registration' },
    ];
    let received: AvailableCounter[] = [];

    staffCounterService.listAvailableCounters().subscribe((list) => (received = list));

    const request = httpTesting.expectOne(`${environment.apiUrl}/api/counters`);
    expect(request.request.method).toBe('GET');
    request.flush(counters);
    expect(received).toEqual(counters);
  });
});
