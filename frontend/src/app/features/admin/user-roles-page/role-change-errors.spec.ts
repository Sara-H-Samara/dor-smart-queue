import { NO_CONNECTION_MESSAGE } from '../../../core/api-failure';
import {
  httpErrorResponseWithMessage,
  httpErrorResponseWithStatus,
} from '../../../testing/http-failure';
import {
  CONFLICT_FALLBACK_MESSAGE,
  GENERIC_ROLE_CHANGE_MESSAGE,
  USER_LIST_FAILED_MESSAGE,
  roleChangeErrorMessage,
  userListErrorMessage,
} from './role-change-errors';

describe('userListErrorMessage', () => {
  it('returns the connection message when the server cannot be reached', () => {
    expect(userListErrorMessage(httpErrorResponseWithStatus(0))).toBe(NO_CONNECTION_MESSAGE);
  });

  it('returns the load failure message for any other error', () => {
    expect(userListErrorMessage(httpErrorResponseWithStatus(500))).toBe(USER_LIST_FAILED_MESSAGE);
    expect(userListErrorMessage(new Error('boom'))).toBe(USER_LIST_FAILED_MESSAGE);
  });
});

describe('roleChangeErrorMessage', () => {
  it('returns the server message for a conflict', () => {
    const conflict = httpErrorResponseWithMessage(
      409,
      "Cancel or finish this student's ticket first.",
    );

    expect(roleChangeErrorMessage(conflict)).toBe("Cancel or finish this student's ticket first.");
  });

  it('returns the fallback message for a conflict without a message', () => {
    expect(roleChangeErrorMessage(httpErrorResponseWithStatus(409))).toBe(
      CONFLICT_FALLBACK_MESSAGE,
    );
  });

  it('returns the fixed message for each other status', () => {
    expect(roleChangeErrorMessage(httpErrorResponseWithStatus(400))).toBe(
      'This role is not valid. Choose Student, Staff or Admin.',
    );
    expect(roleChangeErrorMessage(httpErrorResponseWithStatus(403))).toBe(
      'You do not have access to this page. Please sign in again.',
    );
    expect(roleChangeErrorMessage(httpErrorResponseWithStatus(404))).toBe(
      'This user no longer exists. The list is refreshed.',
    );
    expect(roleChangeErrorMessage(httpErrorResponseWithStatus(0))).toBe(NO_CONNECTION_MESSAGE);
    expect(roleChangeErrorMessage(httpErrorResponseWithStatus(500))).toBe(
      GENERIC_ROLE_CHANGE_MESSAGE,
    );
  });

  it('does not use the server message for a status other than conflict', () => {
    const notFound = httpErrorResponseWithMessage(404, 'User not found.');

    expect(roleChangeErrorMessage(notFound)).toBe(
      'This user no longer exists. The list is refreshed.',
    );
  });

  it('returns the generic message for an error that is not an HTTP error', () => {
    expect(roleChangeErrorMessage(new Error('boom'))).toBe(GENERIC_ROLE_CHANGE_MESSAGE);
  });
});
