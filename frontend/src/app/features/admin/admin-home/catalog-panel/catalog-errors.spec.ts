import { httpErrorResponseWithStatus } from '../../../../testing/http-failure';
import { counterErrorMessageForStatus } from './catalog-errors';

describe('counterErrorMessageForStatus', () => {
  it('says the counter or its service was not found for status 404', () => {
    expect(counterErrorMessageForStatus(httpErrorResponseWithStatus(404))).toBe(
      'This counter or its service was not found. It may have been removed; the list is refreshed.',
    );
  });

  it('asks for an active service and a valid name for status 400', () => {
    expect(counterErrorMessageForStatus(httpErrorResponseWithStatus(400))).toBe(
      'Choose an active service and a name of 1 to 100 characters.',
    );
  });

  it('returns the duplicate-name message for status 409', () => {
    expect(counterErrorMessageForStatus(httpErrorResponseWithStatus(409))).toBe(
      'This name already exists. Please choose a different name.',
    );
  });

  it('returns the generic message for status 500', () => {
    expect(counterErrorMessageForStatus(httpErrorResponseWithStatus(500))).toBe(
      'Something went wrong. Please try again.',
    );
  });
});
