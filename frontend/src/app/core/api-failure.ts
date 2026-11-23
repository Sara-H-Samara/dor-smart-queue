import { HttpErrorResponse } from '@angular/common/http';

export const NO_CONNECTION_MESSAGE = 'Could not reach the server. Check your connection and try again.';

export const UNAUTHORIZED_STATUS = 401;
export const FORBIDDEN_STATUS = 403;
export const NOT_FOUND_STATUS = 404;
export const CONFLICT_STATUS = 409;

export function isHttpStatus(error: unknown, status: number): boolean {
  return error instanceof HttpErrorResponse && error.status === status;
}

export function serverMessageOf(error: unknown, fallbackMessage: string): string {
  if (!(error instanceof HttpErrorResponse)) {
    return fallbackMessage;
  }
  if (error.status === 0) {
    return NO_CONNECTION_MESSAGE;
  }
  const body: unknown = error.error;
  if (typeof body === 'object' && body !== null && 'message' in body) {
    const message = (body as { message: unknown }).message;
    if (typeof message === 'string' && message.trim() !== '') {
      return message;
    }
  }
  return fallbackMessage;
}
