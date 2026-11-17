import { HttpErrorResponse } from '@angular/common/http';

const NOT_FOUND_STATUS = 404;
const CONFLICT_STATUS = 409;

export function isNoActiveTicket(error: unknown): boolean {
  return error instanceof HttpErrorResponse && error.status === NOT_FOUND_STATUS;
}

export function isConflict(error: unknown): boolean {
  return error instanceof HttpErrorResponse && error.status === CONFLICT_STATUS;
}

export function serverMessageOf(error: unknown): string | null {
  if (!(error instanceof HttpErrorResponse)) {
    return null;
  }
  const body: unknown = error.error;
  if (typeof body !== 'object' || body === null || !('message' in body)) {
    return null;
  }
  return typeof body.message === 'string' && body.message.trim() !== '' ? body.message : null;
}

export function messageForTicketFailure(error: unknown): string {
  if (error instanceof HttpErrorResponse && error.status === 0) {
    return 'Could not reach the server. Check your connection and try again.';
  }
  return 'Something went wrong. Please try again.';
}
