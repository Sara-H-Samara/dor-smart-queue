package com.sara.backend.services;

import com.sara.backend.entities.TicketDistance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Turn alert policy")
class TurnAlertPolicyTest {

    private final TurnAlertPolicy defaultLimits = new TurnAlertPolicy(1, 3, 6);

    @Test
    void isTurnNear_nearWithOnePersonAhead_returnsTrue() {
        assertTrue(defaultLimits.isTurnNear(1, TicketDistance.NEAR));
    }

    @Test
    void isTurnNear_nearWithTwoPeopleAhead_returnsFalse() {
        assertFalse(defaultLimits.isTurnNear(2, TicketDistance.NEAR));
    }

    @Test
    void isTurnNear_mediumWithThreePeopleAhead_returnsTrue() {
        assertTrue(defaultLimits.isTurnNear(3, TicketDistance.MEDIUM));
    }

    @Test
    void isTurnNear_mediumWithFourPeopleAhead_returnsFalse() {
        assertFalse(defaultLimits.isTurnNear(4, TicketDistance.MEDIUM));
    }

    @Test
    void isTurnNear_farWithSixPeopleAhead_returnsTrue() {
        assertTrue(defaultLimits.isTurnNear(6, TicketDistance.FAR));
    }

    @Test
    void isTurnNear_farWithSevenPeopleAhead_returnsFalse() {
        assertFalse(defaultLimits.isTurnNear(7, TicketDistance.FAR));
    }

    @Test
    void isTurnNear_withNoPeopleAhead_returnsTrueForEveryDistance() {
        for (TicketDistance distance : TicketDistance.values()) {
            assertTrue(defaultLimits.isTurnNear(0, distance));
        }
    }

    @Test
    void isTurnNear_withFourPeopleAhead_alertsFarButNotNear() {
        assertTrue(defaultLimits.isTurnNear(4, TicketDistance.FAR));
        assertFalse(defaultLimits.isTurnNear(4, TicketDistance.NEAR));
    }

    @Test
    void isTurnNear_withConfiguredLimits_usesConfiguredNumbers() {
        TurnAlertPolicy configuredLimits = new TurnAlertPolicy(0, 2, 10);

        assertFalse(configuredLimits.isTurnNear(1, TicketDistance.NEAR));
        assertTrue(configuredLimits.isTurnNear(2, TicketDistance.MEDIUM));
        assertTrue(configuredLimits.isTurnNear(10, TicketDistance.FAR));
        assertFalse(configuredLimits.isTurnNear(11, TicketDistance.FAR));
    }

    @Test
    void peopleAheadLimitFor_everyDistance_returnsItsConfiguredLimit() {
        assertEquals(1, defaultLimits.peopleAheadLimitFor(TicketDistance.NEAR));
        assertEquals(3, defaultLimits.peopleAheadLimitFor(TicketDistance.MEDIUM));
        assertEquals(6, defaultLimits.peopleAheadLimitFor(TicketDistance.FAR));
    }

    @Test
    void create_withEqualLimits_isAllowed() {
        TurnAlertPolicy equalLimits = new TurnAlertPolicy(2, 2, 2);

        assertEquals(2, equalLimits.peopleAheadLimitFor(TicketDistance.FAR));
    }

    @Test
    void create_withNearLimitAboveMediumLimit_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> new TurnAlertPolicy(4, 3, 6));
    }

    @Test
    void create_withMediumLimitAboveFarLimit_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> new TurnAlertPolicy(1, 7, 6));
    }

    @Test
    void create_withNegativeLimit_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> new TurnAlertPolicy(-1, 3, 6));
    }
}
