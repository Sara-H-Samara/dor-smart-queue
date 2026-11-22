import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { SessionExpiryHandler } from '../live-updates/session-expiry-handler';
import { AuthService } from '../services/auth.service';
import { WAITING_ROOM_PATH } from '../services/waiting-room.service';

const ENDPOINTS_WITHOUT_TOKEN = ['/api/auth/login', '/api/auth/register', WAITING_ROOM_PATH];

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const sessionExpiryHandler = inject(SessionExpiryHandler);
  const token = authService.getToken();
  const isOurApiRequest = request.url.startsWith(`${environment.apiUrl}/api/`);
  const isTokenFreeEndpoint = ENDPOINTS_WITHOUT_TOKEN.some((path) => request.url.endsWith(path));

  if (!token || !isOurApiRequest || isTokenFreeEndpoint) {
    return next(request);
  }

  return next(request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        sessionExpiryHandler.endSessionAndShowLogin();
      }
      return throwError(() => error);
    }),
  );
};
