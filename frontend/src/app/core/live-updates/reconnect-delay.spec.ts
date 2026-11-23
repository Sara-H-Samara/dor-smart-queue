import {
  reconnectDelayInMillisecondsFor,
  reconnectDelayWithJitterInMillisecondsFor,
} from './reconnect-delay';

describe('reconnectDelayWithJitterInMillisecondsFor', () => {
  it('reconnectDelayWithJitter_withRandomFractionZero_returnsBaseDelay', () => {
    expect(reconnectDelayWithJitterInMillisecondsFor(1, 0)).toBe(1000);
    expect(reconnectDelayWithJitterInMillisecondsFor(3, 0)).toBe(4000);
  });

  it('reconnectDelayWithJitter_withRandomFractionNearOne_staysBelowBaseDelayPlusTwentyPercent', () => {
    expect(reconnectDelayWithJitterInMillisecondsFor(1, 0.999999)).toBeLessThan(1200);
    expect(reconnectDelayWithJitterInMillisecondsFor(3, 0.999999)).toBeLessThan(4800);
  });

  it('reconnectDelayWithJitter_withMiddleRandomFraction_returnsBaseDelayPlusPartOfJitter', () => {
    expect(reconnectDelayWithJitterInMillisecondsFor(1, 0.5)).toBe(1100);
    expect(reconnectDelayWithJitterInMillisecondsFor(2, 0.25)).toBe(2100);
  });

  it('reconnectDelayWithJitter_atLongestDelay_staysWithinThirtySecondsAndThirtySixSeconds', () => {
    expect(reconnectDelayWithJitterInMillisecondsFor(6, 0)).toBe(30000);
    expect(reconnectDelayWithJitterInMillisecondsFor(5000, 0.999999)).toBeLessThan(36000);
    expect(reconnectDelayWithJitterInMillisecondsFor(5000, 0.999999)).toBeGreaterThan(30000);
  });
});

describe('reconnectDelayInMillisecondsFor', () => {
  it('waits one second after the first failed attempt', () => {
    expect(reconnectDelayInMillisecondsFor(1)).toBe(1000);
  });

  it('doubles the wait after each further failed attempt', () => {
    expect(reconnectDelayInMillisecondsFor(2)).toBe(2000);
    expect(reconnectDelayInMillisecondsFor(3)).toBe(4000);
    expect(reconnectDelayInMillisecondsFor(4)).toBe(8000);
    expect(reconnectDelayInMillisecondsFor(5)).toBe(16000);
  });

  it('never waits longer than thirty seconds', () => {
    expect(reconnectDelayInMillisecondsFor(6)).toBe(30000);
    expect(reconnectDelayInMillisecondsFor(5000)).toBe(30000);
  });

  it('waits one second when no attempt has failed yet', () => {
    expect(reconnectDelayInMillisecondsFor(0)).toBe(1000);
  });
});
