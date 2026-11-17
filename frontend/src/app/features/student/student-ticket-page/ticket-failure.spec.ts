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
  it('isNoActiveTicket_withNotFoundStatus_returnsTrue', () => {
    expect(isNoActiveTicket(httpErrorResponseWithStatus(404))).toBe(true);
  });

  it('isNoActiveTicket_withConflictStatus_returnsFalse', () => {
    expect(isNoActiveTicket(httpErrorResponseWithStatus(409))).toBe(false);
  });

  it('isNoActiveTicket_withNonHttpError_returnsFalse', () => {
    expect(isNoActiveTicket(new Error('boom'))).toBe(false);
  });
});

describe('isConflict', () => {
  it('isConflict_withConflictStatus_returnsTrue', () => {
    expect(isConflict(httpErrorResponseWithStatus(409))).toBe(true);
  });

  it('isConflict_withNotFoundStatus_returnsFalse', () => {
    expect(isConflict(httpErrorResponseWithStatus(404))).toBe(false);
  });

  it('isConflict_withNonHttpError_returnsFalse', () => {
    expect(isConflict(new Error('boom'))).toBe(false);
  });
});

describe('serverMessageOf', () => {
  it('serverMessageOf_withMessageInBody_returnsMessage', () => {
    const noOpenCounterError = httpErrorResponseWithMessage(
      409,
      'No counter is open for Registration right now.',
    );

    expect(serverMessageOf(noOpenCounterError)).toBe(
      'No counter is open for Registration right now.',
    );
  });

  it('serverMessageOf_withoutBody_returnsNull', () => {
    expect(serverMessageOf(httpErrorResponseWithStatus(409))).toBeNull();
  });

  it('serverMessageOf_withBodyWithoutMessage_returnsNull', () => {
    const errorWithoutMessage = new HttpErrorResponse({ status: 409, error: { status: 409 } });

    expect(serverMessageOf(errorWithoutMessage)).toBeNull();
  });

  it('serverMessageOf_withBlankMessage_returnsNull', () => {
    expect(serverMessageOf(httpErrorResponseWithMessage(409, '   '))).toBeNull();
  });

  it('serverMessageOf_withNonHttpError_returnsNull', () => {
    expect(serverMessageOf(new Error('boom'))).toBeNull();
  });
});

describe('messageForTicketFailure', () => {
  it('messageForTicketFailure_withServerError_returnsGenericMessage', () => {
    expect(messageForTicketFailure(httpErrorResponseWithStatus(500))).toBe(
      'Something went wrong. Please try again.',
    );
  });

  it('messageForTicketFailure_withNetworkFailure_returnsConnectionMessage', () => {
    expect(messageForTicketFailure(httpErrorResponseWithStatus(0))).toBe(
      'Could not reach the server. Check your connection and try again.',
    );
  });

  it('messageForTicketFailure_withNonHttpError_returnsGenericMessage', () => {
    expect(messageForTicketFailure(new Error('boom'))).toBe(
      'Something went wrong. Please try again.',
    );
  });
});
