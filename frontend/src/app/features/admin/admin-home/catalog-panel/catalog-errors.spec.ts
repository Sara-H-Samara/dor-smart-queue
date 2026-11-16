import { httpErrorResponseWithStatus } from '../../../../testing/http-failure';
import { counterErrorMessageForStatus } from './catalog-errors';

describe('counterErrorMessageForStatus', () => {
  it('counterErrorMessageForStatus_with404_saysCounterOrServiceWasNotFound', () => {
    expect(counterErrorMessageForStatus(httpErrorResponseWithStatus(404))).toBe(
      'This counter or its service was not found. It may have been removed; the list is refreshed.',
    );
  });

  it('counterErrorMessageForStatus_with400_asksForActiveServiceAndValidName', () => {
    expect(counterErrorMessageForStatus(httpErrorResponseWithStatus(400))).toBe(
      'Choose an active service and a name of 1 to 100 characters.',
    );
  });

  it('counterErrorMessageForStatus_with409_returnsDuplicateNameMessage', () => {
    expect(counterErrorMessageForStatus(httpErrorResponseWithStatus(409))).toBe(
      'This name already exists. Please choose a different name.',
    );
  });

  it('counterErrorMessageForStatus_with500_returnsGenericMessage', () => {
    expect(counterErrorMessageForStatus(httpErrorResponseWithStatus(500))).toBe(
      'Something went wrong. Please try again.',
    );
  });
});
