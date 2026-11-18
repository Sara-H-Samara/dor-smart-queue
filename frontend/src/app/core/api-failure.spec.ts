import { HttpErrorResponse } from '@angular/common/http';

import { httpErrorResponseWithMessage, httpErrorResponseWithStatus } from '../testing/http-failure';
import { NO_CONNECTION_MESSAGE, isHttpStatus, serverMessageOf } from './api-failure';

const FALLBACK_MESSAGE = 'Something went wrong.';

describe('isHttpStatus', () => {
  it('returns true for a matching status', () => {
    expect(isHttpStatus(httpErrorResponseWithStatus(409), 409)).toBe(true);
  });

  it('returns false for a different status', () => {
    expect(isHttpStatus(httpErrorResponseWithStatus(404), 409)).toBe(false);
  });

  it('returns false for an error that is not an HTTP error', () => {
    expect(isHttpStatus(new Error('boom'), 409)).toBe(false);
  });
});

describe('serverMessageOf', () => {
  it('returns the message from the response body', () => {
    const error = httpErrorResponseWithMessage(409, 'This counter is already serving a student.');

    expect(serverMessageOf(error, FALLBACK_MESSAGE)).toBe('This counter is already serving a student.');
  });

  it('returns the fallback when the response has no body', () => {
    expect(serverMessageOf(httpErrorResponseWithStatus(500), FALLBACK_MESSAGE)).toBe(FALLBACK_MESSAGE);
  });

  it('returns the fallback when the message is blank', () => {
    expect(serverMessageOf(httpErrorResponseWithMessage(409, '   '), FALLBACK_MESSAGE)).toBe(
      FALLBACK_MESSAGE,
    );
  });

  it('returns the fallback when the body is plain text', () => {
    const error = new HttpErrorResponse({ status: 500, error: 'plain text' });

    expect(serverMessageOf(error, FALLBACK_MESSAGE)).toBe(FALLBACK_MESSAGE);
  });

  it('returns the fallback when the message is not a string', () => {
    const error = new HttpErrorResponse({ status: 400, error: { message: 42 } });

    expect(serverMessageOf(error, FALLBACK_MESSAGE)).toBe(FALLBACK_MESSAGE);
  });

  it('returns the no-connection message when the status is 0', () => {
    expect(serverMessageOf(httpErrorResponseWithStatus(0), FALLBACK_MESSAGE)).toBe(NO_CONNECTION_MESSAGE);
  });

  it('returns the fallback for an error that is not an HTTP error', () => {
    expect(serverMessageOf(new Error('boom'), FALLBACK_MESSAGE)).toBe(FALLBACK_MESSAGE);
  });
});
