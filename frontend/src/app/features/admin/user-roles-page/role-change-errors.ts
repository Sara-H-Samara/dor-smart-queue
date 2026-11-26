import { HttpErrorResponse } from '@angular/common/http';

import { NO_CONNECTION_MESSAGE, isHttpStatus, serverMessageOf } from '../../../core/api-failure';

export const CONFLICT_FALLBACK_MESSAGE = 'This role cannot be changed right now.';
export const GENERIC_ROLE_CHANGE_MESSAGE = 'Something went wrong. Please try again.';
export const USER_LIST_FAILED_MESSAGE = 'Could not load the users.';

export function userListErrorMessage(error: unknown): string {
  return isHttpStatus(error, 0) ? NO_CONNECTION_MESSAGE : USER_LIST_FAILED_MESSAGE;
}

export function roleChangeErrorMessage(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return GENERIC_ROLE_CHANGE_MESSAGE;
  }
  switch (error.status) {
    case 0:
      return NO_CONNECTION_MESSAGE;
    case 400:
      return 'This role is not valid. Choose Student, Staff or Admin.';
    case 403:
      return 'You do not have access to this page. Please sign in again.';
    case 404:
      return 'This user no longer exists. The list is refreshed.';
    case 409:
      return serverMessageOf(error, CONFLICT_FALLBACK_MESSAGE);
    default:
      return GENERIC_ROLE_CHANGE_MESSAGE;
  }
}
