import { HttpErrorResponse } from '@angular/common/http';

const EMAIL_ALREADY_REGISTERED_STATUS = 409;

export function registrationErrorMessage(error: HttpErrorResponse): string {
  return error.status === EMAIL_ALREADY_REGISTERED_STATUS
    ? 'This email is already registered.'
    : 'Could not create the account. Check your details and try again.';
}
