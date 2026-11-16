import { HttpErrorResponse } from '@angular/common/http';

export const noActiveServiceMessage =
  'Add or activate a service first. Every counter belongs to one service.';

export function httpStatusOf(err: unknown): number {
  return err instanceof HttpErrorResponse ? err.status : 0;
}

export function counterErrorMessageForStatus(err: unknown): string {
  switch (httpStatusOf(err)) {
    case 404:
      return 'This counter or its service was not found. It may have been removed; the list is refreshed.';
    case 400:
      return 'Choose an active service and a name of 1 to 100 characters.';
    default:
      return errorMessageForStatus(err);
  }
}

export function errorMessageForStatus(err: unknown): string {
  switch (httpStatusOf(err)) {
    case 409:
      return 'This name already exists. Please choose a different name.';
    case 404:
      return 'This item was not found. It may have been removed; the list is refreshed.';
    case 400:
      return 'The name is not valid. Please enter a name of 1 to 100 characters.';
    default:
      return 'Something went wrong. Please try again.';
  }
}

export function findDuplicateCodeMessage(err: unknown, supportsCode: boolean): string | null {
  if (!supportsCode || !(err instanceof HttpErrorResponse) || err.status !== 409) {
    return null;
  }
  const body = err.error as { message?: unknown } | string | null;
  const message = typeof body === 'string' ? body : body?.message;
  return typeof message === 'string' && /\bcode\b/i.test(message) && /already used/i.test(message)
    ? message
    : null;
}
