const FIRST_RECONNECT_DELAY_IN_MILLISECONDS = 1000;
const LONGEST_RECONNECT_DELAY_IN_MILLISECONDS = 30000;
const MAXIMUM_JITTER_FRACTION = 0.2;

export function reconnectDelayWithJitterInMillisecondsFor(
  failedAttemptCount: number,
  randomFraction: number,
): number {
  const baseDelay = reconnectDelayInMillisecondsFor(failedAttemptCount);
  return baseDelay + baseDelay * MAXIMUM_JITTER_FRACTION * randomFraction;
}

export function reconnectDelayInMillisecondsFor(failedAttemptCount: number): number {
  const doublingsSoFar = Math.max(0, failedAttemptCount - 1);
  return Math.min(
    LONGEST_RECONNECT_DELAY_IN_MILLISECONDS,
    FIRST_RECONNECT_DELAY_IN_MILLISECONDS * 2 ** doublingsSoFar,
  );
}
