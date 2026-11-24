package com.sara.backend.streams;

import com.sara.backend.exceptions.StreamCapacityReachedException;
import com.sara.backend.exceptions.TooManyStreamsFromAddressException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Stream connection limiter")
class StreamConnectionLimiterTest {

    private static final String FIRST_ADDRESS = "10.0.0.1";
    private static final String SECOND_ADDRESS = "10.0.0.2";
    private static final String THIRD_ADDRESS = "10.0.0.3";
    private static final int CONCURRENT_ATTEMPT_COUNT = 200;
    private static final int CONCURRENT_ROUND_COUNT = 30;

    @Test
    void reserveSlot_belowBothLimits_returnsReservation() {
        StreamConnectionLimiter limiter = new StreamConnectionLimiter(3, 2);

        assertDoesNotThrow(() -> limiter.reserveSlot(FIRST_ADDRESS));
    }

    @Test
    void reserveSlot_whenTotalLimitReached_throwsStreamCapacityReached() {
        StreamConnectionLimiter limiter = new StreamConnectionLimiter(2, 5);
        limiter.reserveSlot(FIRST_ADDRESS);
        limiter.reserveSlot(SECOND_ADDRESS);

        assertThrows(StreamCapacityReachedException.class, () -> limiter.reserveSlot(THIRD_ADDRESS));
    }

    @Test
    void reserveSlot_whenAddressLimitReached_throwsTooManyStreamsFromAddress() {
        StreamConnectionLimiter limiter = new StreamConnectionLimiter(10, 2);
        limiter.reserveSlot(FIRST_ADDRESS);
        limiter.reserveSlot(FIRST_ADDRESS);

        assertThrows(TooManyStreamsFromAddressException.class, () -> limiter.reserveSlot(FIRST_ADDRESS));
    }

    @Test
    void reserveSlot_whenBothLimitsReached_throwsTooManyStreamsFromAddress() {
        StreamConnectionLimiter limiter = new StreamConnectionLimiter(2, 2);
        limiter.reserveSlot(FIRST_ADDRESS);
        limiter.reserveSlot(FIRST_ADDRESS);

        assertThrows(TooManyStreamsFromAddressException.class, () -> limiter.reserveSlot(FIRST_ADDRESS));
    }

    @Test
    void reserveSlot_whenOneAddressIsFull_stillAcceptsAnotherAddress() {
        StreamConnectionLimiter limiter = new StreamConnectionLimiter(10, 2);
        limiter.reserveSlot(FIRST_ADDRESS);
        limiter.reserveSlot(FIRST_ADDRESS);

        assertDoesNotThrow(() -> limiter.reserveSlot(SECOND_ADDRESS));
    }

    @Test
    void reserveSlot_afterTotalRefusal_doesNotTrackTheRefusedAddress() {
        StreamConnectionLimiter limiter = new StreamConnectionLimiter(2, 5);
        limiter.reserveSlot(FIRST_ADDRESS);
        limiter.reserveSlot(SECOND_ADDRESS);

        assertThrows(StreamCapacityReachedException.class, () -> limiter.reserveSlot(THIRD_ADDRESS));

        assertEquals(2, limiter.trackedAddressCount());
    }

    @Test
    void reserveSlot_afterAddressRefusal_doesNotCountTheRefusedAttempt() {
        StreamConnectionLimiter limiter = new StreamConnectionLimiter(3, 1);
        StreamReservation firstReservation = limiter.reserveSlot(FIRST_ADDRESS);
        assertThrows(TooManyStreamsFromAddressException.class, () -> limiter.reserveSlot(FIRST_ADDRESS));
        limiter.reserveSlot(SECOND_ADDRESS);

        firstReservation.release();

        assertDoesNotThrow(() -> limiter.reserveSlot(FIRST_ADDRESS));
        assertDoesNotThrow(() -> limiter.reserveSlot(THIRD_ADDRESS));
    }

    @Test
    void release_afterReservation_makesRoomAgain() {
        StreamConnectionLimiter limiter = new StreamConnectionLimiter(1, 1);
        StreamReservation reservation = limiter.reserveSlot(FIRST_ADDRESS);

        reservation.release();

        assertDoesNotThrow(() -> limiter.reserveSlot(FIRST_ADDRESS));
    }

    @Test
    void release_calledTwice_freesOnlyOneSlot() {
        StreamConnectionLimiter limiter = new StreamConnectionLimiter(2, 5);
        StreamReservation firstReservation = limiter.reserveSlot(FIRST_ADDRESS);
        limiter.reserveSlot(FIRST_ADDRESS);

        firstReservation.release();
        firstReservation.release();

        limiter.reserveSlot(SECOND_ADDRESS);
        assertThrows(StreamCapacityReachedException.class, () -> limiter.reserveSlot(THIRD_ADDRESS));
    }

    @Test
    void release_whenAddressCountReachesZero_removesTheAddress() {
        StreamConnectionLimiter limiter = new StreamConnectionLimiter(10, 5);
        StreamReservation firstReservation = limiter.reserveSlot(FIRST_ADDRESS);
        StreamReservation secondReservation = limiter.reserveSlot(SECOND_ADDRESS);

        firstReservation.release();
        secondReservation.release();

        assertEquals(0, limiter.trackedAddressCount());
    }

    @Test
    void release_whenAddressStillHasOpenStreams_keepsTheAddress() {
        StreamConnectionLimiter limiter = new StreamConnectionLimiter(10, 5);
        StreamReservation firstReservation = limiter.reserveSlot(FIRST_ADDRESS);
        limiter.reserveSlot(FIRST_ADDRESS);

        firstReservation.release();

        assertEquals(1, limiter.trackedAddressCount());
    }

    @Test
    void reserveSlot_fromTwoHundredThreads_neverExceedsTotalLimit() throws InterruptedException {
        for (int round = 0; round < CONCURRENT_ROUND_COUNT; round++) {
            StreamConnectionLimiter limiter = new StreamConnectionLimiter(100, 1000);

            int acceptedCount = countAcceptedReservationsMadeAtTheSameTime(
                    limiter, attemptNumber -> "10.1.0." + attemptNumber);

            assertEquals(100, acceptedCount);
        }
    }

    @Test
    void reserveSlot_fromTwoHundredThreadsWithOneAddress_neverExceedsAddressLimit() throws InterruptedException {
        for (int round = 0; round < CONCURRENT_ROUND_COUNT; round++) {
            StreamConnectionLimiter limiter = new StreamConnectionLimiter(1000, 100);

            int acceptedCount = countAcceptedReservationsMadeAtTheSameTime(
                    limiter, attemptNumber -> FIRST_ADDRESS);

            assertEquals(100, acceptedCount);
        }
    }

    private int countAcceptedReservationsMadeAtTheSameTime(
            StreamConnectionLimiter limiter, IntFunction<String> addressForAttempt) throws InterruptedException {
        AtomicInteger acceptedCount = new AtomicInteger();
        AtomicInteger refusedCount = new AtomicInteger();
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch allAttemptsFinished = new CountDownLatch(CONCURRENT_ATTEMPT_COUNT);
        try (ExecutorService attemptExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int attemptNumber = 0; attemptNumber < CONCURRENT_ATTEMPT_COUNT; attemptNumber++) {
                String clientAddress = addressForAttempt.apply(attemptNumber);
                attemptExecutor.execute(() -> {
                    try {
                        startSignal.await();
                        limiter.reserveSlot(clientAddress);
                        acceptedCount.incrementAndGet();
                    } catch (StreamCapacityReachedException | TooManyStreamsFromAddressException refused) {
                        refusedCount.incrementAndGet();
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    } finally {
                        allAttemptsFinished.countDown();
                    }
                });
            }
            startSignal.countDown();
            allAttemptsFinished.await();
        }
        assertEquals(CONCURRENT_ATTEMPT_COUNT, acceptedCount.get() + refusedCount.get());
        return acceptedCount.get();
    }
}
