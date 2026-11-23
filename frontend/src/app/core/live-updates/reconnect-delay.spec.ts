import { reconnectDelayInMillisecondsFor } from './reconnect-delay';

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
