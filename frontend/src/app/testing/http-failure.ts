import { HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';

export function httpErrorResponseWithStatus(status: number): HttpErrorResponse {
  return new HttpErrorResponse({ status });
}

export function httpErrorResponseWithMessage(status: number, message: string): HttpErrorResponse {
  return new HttpErrorResponse({ status, error: { message } });
}

export function failingRequestWithMessage(status: number, message: string): Observable<never> {
  return throwError(() => httpErrorResponseWithMessage(status, message));
}

export function failingRequestWithStatus(status: number): Observable<never> {
  return throwError(() => httpErrorResponseWithStatus(status));
}
