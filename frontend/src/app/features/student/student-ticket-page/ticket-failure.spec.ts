import { HttpErrorResponse } from '@angular/common/http';

import { isNoActiveTicket, isTicketAlreadyTaken, messageForTicketFailure } from './ticket-failure';

function httpFailureWithStatus(status: number): HttpErrorResponse {
  return new HttpErrorResponse({ status });
}

describe('isNoActiveTicket', () => {
  it('isNoActiveTicket_withNotFoundStatus_returnsTrue', () => {
    expect(isNoActiveTicket(httpFailureWithStatus(404))).toBe(true);
  });

  it('isNoActiveTicket_withConflictStatus_returnsFalse', () => {
    expect(isNoActiveTicket(httpFailureWithStatus(409))).toBe(false);
  });

  it('isNoActiveTicket_withNonHttpError_returnsFalse', () => {
    expect(isNoActiveTicket(new Error('boom'))).toBe(false);
  });
});

describe('isTicketAlreadyTaken', () => {
  it('isTicketAlreadyTaken_withConflictStatus_returnsTrue', () => {
    expect(isTicketAlreadyTaken(httpFailureWithStatus(409))).toBe(true);
  });

  it('isTicketAlreadyTaken_withNotFoundStatus_returnsFalse', () => {
    expect(isTicketAlreadyTaken(httpFailureWithStatus(404))).toBe(false);
  });

  it('isTicketAlreadyTaken_withNonHttpError_returnsFalse', () => {
    expect(isTicketAlreadyTaken(new Error('boom'))).toBe(false);
  });
});

describe('messageForTicketFailure', () => {
  it('messageForTicketFailure_withServerError_returnsGenericMessage', () => {
    expect(messageForTicketFailure(httpFailureWithStatus(500))).toBe(
      'Something went wrong. Please try again.',
    );
  });

  it('messageForTicketFailure_withNetworkFailure_returnsConnectionMessage', () => {
    expect(messageForTicketFailure(httpFailureWithStatus(0))).toBe(
      'Could not reach the server. Check your connection and try again.',
    );
  });

  it('messageForTicketFailure_withNonHttpError_returnsGenericMessage', () => {
    expect(messageForTicketFailure(new Error('boom'))).toBe(
      'Something went wrong. Please try again.',
    );
  });
});
