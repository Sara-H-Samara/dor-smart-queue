import { HttpErrorResponse } from '@angular/common/http';

const NOT_FOUND_STATUS = 404;
const CONFLICT_STATUS = 409;

export function isNoActiveTicket(error: unknown): boolean {
  return error instanceof HttpErrorResponse && error.status === NOT_FOUND_STATUS;
}

export function isTicketAlreadyTaken(error: unknown): boolean {
  return error instanceof HttpErrorResponse && error.status === CONFLICT_STATUS;
}

export function messageForTicketFailure(error: unknown): string {
  if (error instanceof HttpErrorResponse && error.status === 0) {
    return 'Could not reach the server. Check your connection and try again.';
  }
  return 'Something went wrong. Please try again.';
}
