package com.sara.backend.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Estimated wait calculator")
class EstimatedWaitCalculatorTest {

    private final EstimatedWaitCalculator defaultSettings = new EstimatedWaitCalculator(5, 10, 3, 30, 60);

    private static List<Duration> minutes(long... minuteValues) {
        return Arrays.stream(minuteValues).mapToObj(Duration::ofMinutes).toList();
    }

    @Test
    void estimateWaitMinutes_withNoDurations_returnsDefaultMinutesTimesPosition() {
        assertEquals(15, defaultSettings.estimateWaitMinutes(3, List.of()));
    }

    @Test
    void estimateWaitMinutes_belowMinimumSamples_returnsDefaultMinutesTimesPosition() {
        assertEquals(10, defaultSettings.estimateWaitMinutes(2, minutes(10, 10)));
    }

    @Test
    void estimateWaitMinutes_atMinimumSamples_returnsAverageTimesPosition() {
        assertEquals(20, defaultSettings.estimateWaitMinutes(2, minutes(10, 10, 10)));
    }

    @Test
    void estimateWaitMinutes_withEnoughSamples_returnsAverageTimesPosition() {
        assertEquals(18, defaultSettings.estimateWaitMinutes(3, minutes(4, 6, 8, 6)));
    }

    @Test
    void estimateWaitMinutes_withFractionalResult_roundsUp() {
        List<Duration> durations = List.of(
                Duration.ofSeconds(60), Duration.ofSeconds(60), Duration.ofSeconds(61));

        assertEquals(2, defaultSettings.estimateWaitMinutes(1, durations));
    }

    @Test
    void estimateWaitMinutes_withWholeMinuteResult_doesNotRoundUp() {
        assertEquals(2, defaultSettings.estimateWaitMinutes(1, minutes(2, 2, 2)));
    }

    @Test
    void estimateWaitMinutes_withDurationBelowShortest_ignoresIt() {
        List<Duration> durations = List.of(
                Duration.ofMinutes(10), Duration.ofMinutes(10), Duration.ofMinutes(10), Duration.ofSeconds(10));

        assertEquals(10, defaultSettings.estimateWaitMinutes(1, durations));
    }

    @Test
    void estimateWaitMinutes_withDurationAboveLongest_ignoresIt() {
        assertEquals(10, defaultSettings.estimateWaitMinutes(1, minutes(10, 10, 10, 180)));
    }

    @Test
    void estimateWaitMinutes_withNegativeDuration_ignoresIt() {
        assertEquals(10, defaultSettings.estimateWaitMinutes(1, minutes(10, 10, 10, -5)));
    }

    @Test
    void estimateWaitMinutes_withNegativeDurationAndShortestZero_ignoresIt() {
        EstimatedWaitCalculator shortestZero = new EstimatedWaitCalculator(5, 10, 3, 0, 60);

        assertEquals(10, shortestZero.estimateWaitMinutes(1, minutes(10, 10, 10, -5)));
    }

    @Test
    void estimateWaitMinutes_whenIgnoredDurationsLeaveTooFewSamples_returnsDefaultMinutesTimesPosition() {
        assertEquals(5, defaultSettings.estimateWaitMinutes(1, minutes(10, 10, 180)));
    }

    @Test
    void estimateWaitMinutes_withDurationsExactlyAtShortestAndLongest_usesThem() {
        List<Duration> durations = List.of(
                Duration.ofSeconds(30), Duration.ofMinutes(60), Duration.ofMinutes(60));

        assertEquals(41, defaultSettings.estimateWaitMinutes(1, durations));
    }

    @Test
    void estimateWaitMinutes_withConfiguredSettings_usesConfiguredNumbers() {
        EstimatedWaitCalculator configured = new EstimatedWaitCalculator(8, 4, 2, 0, 30);

        assertEquals(16, configured.estimateWaitMinutes(2, minutes(40)));
        assertEquals(6, configured.estimateWaitMinutes(1, minutes(6, 6)));
    }

    @Test
    void recentTicketCount_withConfiguredSettings_returnsConfiguredNumber() {
        assertEquals(4, new EstimatedWaitCalculator(8, 4, 2, 0, 30).recentTicketCount());
    }

    @Test
    void create_withMinimumSamplesEqualToRecentCount_isAllowed() {
        assertEquals(3, new EstimatedWaitCalculator(5, 3, 3, 30, 60).recentTicketCount());
    }

    @Test
    void create_withShortestZero_isAllowed() {
        assertEquals(10, new EstimatedWaitCalculator(5, 10, 3, 0, 60).recentTicketCount());
    }

    @Test
    void create_withDefaultMinutesBelowOne_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> new EstimatedWaitCalculator(0, 10, 3, 30, 60));
    }

    @Test
    void create_withRecentCountBelowOne_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> new EstimatedWaitCalculator(5, 0, 1, 30, 60));
    }

    @Test
    void create_withMinimumSamplesBelowOne_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> new EstimatedWaitCalculator(5, 10, 0, 30, 60));
    }

    @Test
    void create_withMinimumSamplesAboveRecentCount_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> new EstimatedWaitCalculator(5, 3, 4, 30, 60));
    }

    @Test
    void create_withNegativeShortestSeconds_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> new EstimatedWaitCalculator(5, 10, 3, -1, 60));
    }

    @Test
    void create_withLongestEqualToShortest_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> new EstimatedWaitCalculator(5, 10, 3, 60, 1));
    }

    @Test
    void create_withLongestBelowShortest_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> new EstimatedWaitCalculator(5, 10, 3, 120, 1));
    }

    @Test
    void create_withLongestZero_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> new EstimatedWaitCalculator(5, 10, 3, 0, 0));
    }
}
