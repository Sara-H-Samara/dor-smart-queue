package com.sara.backend.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class EstimatedWaitCalculator {

    private static final int SECONDS_PER_MINUTE = 60;

    private final int defaultMinutesPerPerson;
    private final int recentTicketCount;
    private final int minimumSamples;
    private final int shortestServiceSeconds;
    private final int longestServiceMinutes;

    public EstimatedWaitCalculator(
            @Value("${dor.estimated-wait.default-minutes:5}") int defaultMinutesPerPerson,
            @Value("${dor.estimated-wait.recent-count:10}") int recentTicketCount,
            @Value("${dor.estimated-wait.minimum-samples:3}") int minimumSamples,
            @Value("${dor.estimated-wait.shortest-service-seconds:30}") int shortestServiceSeconds,
            @Value("${dor.estimated-wait.longest-service-minutes:60}") int longestServiceMinutes) {
        if (defaultMinutesPerPerson < 1) {
            throw new IllegalArgumentException("The default minutes per person must be at least 1.");
        }
        if (recentTicketCount < 1) {
            throw new IllegalArgumentException("The recent ticket count must be at least 1.");
        }
        if (minimumSamples < 1 || minimumSamples > recentTicketCount) {
            throw new IllegalArgumentException(
                    "The minimum samples must be at least 1 and not more than the recent ticket count.");
        }
        if (shortestServiceSeconds < 0
                || (long) longestServiceMinutes * SECONDS_PER_MINUTE <= shortestServiceSeconds) {
            throw new IllegalArgumentException(
                    "The shortest service time must not be negative and must be below the longest service time.");
        }
        this.defaultMinutesPerPerson = defaultMinutesPerPerson;
        this.recentTicketCount = recentTicketCount;
        this.minimumSamples = minimumSamples;
        this.shortestServiceSeconds = shortestServiceSeconds;
        this.longestServiceMinutes = longestServiceMinutes;
    }

    public int recentTicketCount() {
        return recentTicketCount;
    }

    public int estimateWaitMinutes(int position, List<Duration> recentServiceDurations) {
        List<Duration> usableDurations = recentServiceDurations.stream()
                .filter(this::isUsableServiceDuration)
                .toList();
        if (usableDurations.size() < minimumSamples) {
            return position * defaultMinutesPerPerson;
        }
        long totalSeconds = usableDurations.stream().mapToLong(Duration::getSeconds).sum();
        long secondsForAllPositions = position * totalSeconds;
        long divisorForAverageMinutes = (long) SECONDS_PER_MINUTE * usableDurations.size();
        return (int) Math.ceilDiv(secondsForAllPositions, divisorForAverageMinutes);
    }

    private boolean isUsableServiceDuration(Duration serviceDuration) {
        long seconds = serviceDuration.getSeconds();
        return seconds >= shortestServiceSeconds
                && seconds <= (long) longestServiceMinutes * SECONDS_PER_MINUTE;
    }
}
