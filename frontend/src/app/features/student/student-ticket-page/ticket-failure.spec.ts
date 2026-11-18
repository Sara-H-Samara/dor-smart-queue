import { HttpErrorResponse } from '@angular/common/http';

import {
  httpErrorResponseWithMessage,
  httpErrorResponseWithStatus,
} from '../../../testing/http-failure';
import {
  isConflict,
  isNoActiveTicket,
  messageForTicketFailure,
  serverMessageOf,
} from './ticket-failure';

describe('isNoActiveTicket', () => {
  it('returns true for a not-found status', () => {
    expect(isNoActiveTicket(httpErrorResponseWithStatus(404))).toBe(true);
  });

  it('returns false for a conflict status', () => {
    expect(isNoActiveTicket(httpErrorResponseWithStatus(409))).toBe(false);
  });

  it('returns false for an error that is not an HTTP error', () => {
    expect(isNoActiveTicket(new Error('boom'))).toBe(false);
  });
});

describe('isConflict', () => {
  it('returns true for a conflict status', () => {
    expect(isConflict(httpErrorResponseWithStatus(409))).toBe(true);
  });

  it('returns false for a not-found status', () => {
    expect(isConflict(httpErrorResponseWithStatus(404))).toBe(false);
  });

  it('returns false for an error that is not an HTTP error', () => {
    expect(isConflict(new Error('boom'))).toBe(false);
  });
});

describe('serverMessageOf for tickets', () => {
  it('returns the message from the response body', () => {
    const noOpenCounterError = httpErrorResponseWithMessage(
      409,
      'No counter is open for Registration right now.',
    );

    expect(serverMessageOf(noOpenCounterError)).toBe(
      'No counter is open for Registration right now.',
    );
  });

  it('returns null when the response has no body', () => {
    expect(serverMessageOf(httpErrorResponseWithStatus(409))).toBeNull();
  });

  it('returns null when the body has no message', () => {
    const errorWithoutMessage = new HttpErrorResponse({ status: 409, error: { status: 409 } });

    expect(serverMessageOf(errorWithoutMessage)).toBeNull();
  });

  it('returns null when the message is blank', () => {
    expect(serverMessageOf(httpErrorResponseWithMessage(409, '   '))).toBeNull();
  });

  it('returns null for an error that is not an HTTP error', () => {
    expect(serverMessageOf(new Error('boom'))).toBeNull();
  });
});

describe('messageForTicketFailure', () => {
  it('returns the generic message for a server error', () => {
    expect(messageForTicketFailure(httpErrorResponseWithStatus(500))).toBe(
      'Something went wrong. Please try again.',
    );
  });

  it('returns the connection message when the network fails', () => {
    expect(messageForTicketFailure(httpErrorResponseWithStatus(0))).toBe(
      'Could not reach the server. Check your connection and try again.',
    );
  });

  it('returns the generic message for an error that is not an HTTP error', () => {
    expect(messageForTicketFailure(new Error('boom'))).toBe(
      'Something went wrong. Please try again.',
    );
  });
});
