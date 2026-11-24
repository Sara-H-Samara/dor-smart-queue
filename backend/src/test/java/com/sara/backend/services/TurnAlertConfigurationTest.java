package com.sara.backend.services;

import com.sara.backend.entities.TicketDistance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Turn alert configuration")
class TurnAlertConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(TurnAlertPolicy.class);

    @Test
    void peopleAheadLimitFor_withConfiguredProperties_returnsConfiguredNumbers() {
        contextRunner
                .withPropertyValues(
                        "dor.turn-alert.near-people-ahead=2",
                        "dor.turn-alert.medium-people-ahead=4",
                        "dor.turn-alert.far-people-ahead=9")
                .run(context -> {
                    TurnAlertPolicy turnAlertPolicy = context.getBean(TurnAlertPolicy.class);
                    assertEquals(2, turnAlertPolicy.peopleAheadLimitFor(TicketDistance.NEAR));
                    assertEquals(4, turnAlertPolicy.peopleAheadLimitFor(TicketDistance.MEDIUM));
                    assertEquals(9, turnAlertPolicy.peopleAheadLimitFor(TicketDistance.FAR));
                });
    }

    @Test
    void peopleAheadLimitFor_withoutProperties_returnsOneThreeAndSix() {
        contextRunner.run(context -> {
            TurnAlertPolicy turnAlertPolicy = context.getBean(TurnAlertPolicy.class);
            assertEquals(1, turnAlertPolicy.peopleAheadLimitFor(TicketDistance.NEAR));
            assertEquals(3, turnAlertPolicy.peopleAheadLimitFor(TicketDistance.MEDIUM));
            assertEquals(6, turnAlertPolicy.peopleAheadLimitFor(TicketDistance.FAR));
        });
    }

    @Test
    void createBean_withNearLimitAboveMediumLimit_failsToStart() {
        contextRunner
                .withPropertyValues(
                        "dor.turn-alert.near-people-ahead=5",
                        "dor.turn-alert.medium-people-ahead=3")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }
}
