import { HttpErrorResponse } from '@angular/common/http';

import { httpErrorResponseWithMessage, httpErrorResponseWithStatus } from '../testing/http-failure';
import { NO_CONNECTION_MESSAGE, isHttpStatus, serverMessageOf } from './api-failure';

const FALLBACK_MESSAGE = 'Something went wrong.';

describe('isHttpStatus', () => {
  it('isHttpStatus_withMatchingStatus_returnsTrue', () => {
    expect(isHttpStatus(httpErrorResponseWithStatus(409), 409)).toBe(true);
  });

  it('isHttpStatus_withOtherStatus_returnsFalse', () => {
    expect(isHttpStatus(httpErrorResponseWithStatus(404), 409)).toBe(false);
  });

  it('isHttpStatus_withNonHttpError_returnsFalse', () => {
    expect(isHttpStatus(new Error('boom'), 409)).toBe(false);
  });
});

describe('serverMessageOf', () => {
  it('serverMessageOf_withMessageInBody_returnsServerMessage', () => {
    const error = httpErrorResponseWithMessage(409, 'This counter is already serving a student.');

    expect(serverMessageOf(error, FALLBACK_MESSAGE)).toBe('This counter is already serving a student.');
  });

  it('serverMessageOf_withoutBody_returnsFallback', () => {
    expect(serverMessageOf(httpErrorResponseWithStatus(500), FALLBACK_MESSAGE)).toBe(FALLBACK_MESSAGE);
  });

  it('serverMessageOf_withBlankMessage_returnsFallback', () => {
    expect(serverMessageOf(httpErrorResponseWithMessage(409, '   '), FALLBACK_MESSAGE)).toBe(
      FALLBACK_MESSAGE,
    );
  });

  it('serverMessageOf_withTextBody_returnsFallback', () => {
    const error = new HttpErrorResponse({ status: 500, error: 'plain text' });

    expect(serverMessageOf(error, FALLBACK_MESSAGE)).toBe(FALLBACK_MESSAGE);
  });

  it('serverMessageOf_withNonStringMessage_returnsFallback', () => {
    const error = new HttpErrorResponse({ status: 400, error: { message: 42 } });

    expect(serverMessageOf(error, FALLBACK_MESSAGE)).toBe(FALLBACK_MESSAGE);
  });

  it('serverMessageOf_withStatusZero_returnsNoConnectionMessage', () => {
    expect(serverMessageOf(httpErrorResponseWithStatus(0), FALLBACK_MESSAGE)).toBe(NO_CONNECTION_MESSAGE);
  });

  it('serverMessageOf_withNonHttpError_returnsFallback', () => {
    expect(serverMessageOf(new Error('boom'), FALLBACK_MESSAGE)).toBe(FALLBACK_MESSAGE);
  });
});
