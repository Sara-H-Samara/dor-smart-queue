import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { SessionExpiryHandler } from '../live-updates/session-expiry-handler';
import { AuthService } from '../services/auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpTesting: HttpTestingController;
  let endSessionAndShowLogin: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    endSessionAndShowLogin = vi.fn();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { getToken: () => 'saved-token' } },
        { provide: SessionExpiryHandler, useValue: { endSessionAndShowLogin } },
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

  it('ends the session when the server answers 401', () => {
    httpClient.get(`${environment.apiUrl}/api/services`).subscribe({ error: () => undefined });

    httpTesting
      .expectOne(`${environment.apiUrl}/api/services`)
      .flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(endSessionAndShowLogin).toHaveBeenCalledTimes(1);
  });

  it('passes the 401 error on to the caller', () => {
    let failureStatus: number | undefined;
    httpClient.get(`${environment.apiUrl}/api/services`).subscribe({
      error: (error: { status: number }) => (failureStatus = error.status),
    });

    httpTesting
      .expectOne(`${environment.apiUrl}/api/services`)
      .flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(failureStatus).toBe(401);
  });

  it('does not end the session when the server answers 403', () => {
    httpClient.get(`${environment.apiUrl}/api/services`).subscribe({ error: () => undefined });

    httpTesting
      .expectOne(`${environment.apiUrl}/api/services`)
      .flush(null, { status: 403, statusText: 'Forbidden' });

    expect(endSessionAndShowLogin).not.toHaveBeenCalled();
  });
});
