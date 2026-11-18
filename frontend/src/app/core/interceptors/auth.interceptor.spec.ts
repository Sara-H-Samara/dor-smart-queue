import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { getToken: () => 'saved-token', logout: vi.fn() } },
        { provide: Router, useValue: { navigate: vi.fn() } },
      ],
    });
    httpClient = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('sends no Authorization header on the public queue status request', () => {
    httpClient.get(`${environment.apiUrl}/api/public/queue-status`).subscribe();

    const request = httpTesting.expectOne(`${environment.apiUrl}/api/public/queue-status`);
    expect(request.request.headers.has('Authorization')).toBe(false);
  });

  it('sends the Bearer token on another API request', () => {
    httpClient.get(`${environment.apiUrl}/api/services`).subscribe();

    const request = httpTesting.expectOne(`${environment.apiUrl}/api/services`);
    expect(request.request.headers.get('Authorization')).toBe('Bearer saved-token');
  });
});
