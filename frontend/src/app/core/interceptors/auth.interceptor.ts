import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';

const PUBLIC_PATHS = ['/api/auth/login', '/api/auth/register'];

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();
  const isApiCall = request.url.startsWith(`${environment.apiUrl}/api/`);
  const isPublic = PUBLIC_PATHS.some((path) => request.url.endsWith(path));

  // Add the Bearer token only to our own API calls, never to login or register.
  if (!token || !isApiCall || isPublic) {
    return next(request);
  }

  return next(
    request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }),
  ).pipe(
    catchError((error: unknown) => {
      // 401 means the token is rejected or expired, so sign out and go to login.
      if (error instanceof HttpErrorResponse && error.status === 401) {
        authService.logout();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    }),
  );
};
