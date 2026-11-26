package com.sara.backend.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Estimated wait configuration")
class EstimatedWaitConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(EstimatedWaitCalculator.class);

    @Test
    void estimateWaitMinutes_withoutProperties_usesFiveMinutesAndTenRecentTickets() {
        contextRunner.run(context -> {
            EstimatedWaitCalculator calculator = context.getBean(EstimatedWaitCalculator.class);
            assertEquals(10, calculator.recentTicketCount());
            assertEquals(10, calculator.estimateWaitMinutes(2, List.of()));
        });
    }

    @Test
    void estimateWaitMinutes_withoutProperties_needsThreeSamples() {
        contextRunner.run(context -> {
            EstimatedWaitCalculator calculator = context.getBean(EstimatedWaitCalculator.class);
            List<Duration> twoSamples = List.of(Duration.ofMinutes(10), Duration.ofMinutes(10));
            List<Duration> threeSamples = List.of(
                    Duration.ofMinutes(10), Duration.ofMinutes(10), Duration.ofMinutes(10));
            assertEquals(5, calculator.estimateWaitMinutes(1, twoSamples));
            assertEquals(10, calculator.estimateWaitMinutes(1, threeSamples));
        });
    }

    @Test
    void estimateWaitMinutes_withoutProperties_ignoresDurationsBelowThirtySecondsAndAboveSixtyMinutes() {
        contextRunner.run(context -> {
            EstimatedWaitCalculator calculator = context.getBean(EstimatedWaitCalculator.class);
            List<Duration> durations = List.of(
                    Duration.ofMinutes(10),
                    Duration.ofMinutes(10),
                    Duration.ofMinutes(10),
                    Duration.ofSeconds(29),
                    Duration.ofMinutes(61));
            assertEquals(10, calculator.estimateWaitMinutes(1, durations));
        });
    }

    @Test
    void estimateWaitMinutes_withConfiguredProperties_usesConfiguredNumbers() {
        contextRunner
                .withPropertyValues(
                        "dor.estimated-wait.default-minutes=8",
                        "dor.estimated-wait.recent-count=4",
                        "dor.estimated-wait.minimum-samples=2",
                        "dor.estimated-wait.shortest-service-seconds=0",
                        "dor.estimated-wait.longest-service-minutes=30")
                .run(context -> {
                    EstimatedWaitCalculator calculator = context.getBean(EstimatedWaitCalculator.class);
                    assertEquals(4, calculator.recentTicketCount());
                    assertEquals(16, calculator.estimateWaitMinutes(2, List.of(Duration.ofMinutes(40))));
                    List<Duration> twoSamples = List.of(Duration.ofMinutes(6), Duration.ofMinutes(6));
                    List<Duration> zeroLengthSampleIncluded = List.of(
                            Duration.ofMinutes(6), Duration.ofMinutes(6), Duration.ZERO);
                    assertEquals(6, calculator.estimateWaitMinutes(1, twoSamples));
                    assertEquals(4, calculator.estimateWaitMinutes(1, zeroLengthSampleIncluded));
                });
    }

    @Test
    void createBean_withMinimumSamplesAboveRecentCount_failsToStart() {
        contextRunner
                .withPropertyValues(
                        "dor.estimated-wait.recent-count=2",
                        "dor.estimated-wait.minimum-samples=3")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test
    void createBean_withDefaultMinutesBelowOne_failsToStart() {
        contextRunner
                .withPropertyValues("dor.estimated-wait.default-minutes=0")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test
    void createBean_withNegativeShortestServiceSeconds_failsToStart() {
        contextRunner
                .withPropertyValues("dor.estimated-wait.shortest-service-seconds=-1")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test
    void createBean_withLongestServiceNotAboveShortest_failsToStart() {
        contextRunner
                .withPropertyValues(
                        "dor.estimated-wait.shortest-service-seconds=120",
                        "dor.estimated-wait.longest-service-minutes=2")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }
}
