import { HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';

export function httpErrorResponseWithStatus(status: number): HttpErrorResponse {
  return new HttpErrorResponse({ status });
}

export function failingRequestWithStatus(status: number): Observable<never> {
  return throwError(() => httpErrorResponseWithStatus(status));
}
