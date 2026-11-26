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

    private static final int ONE_OPEN_COUNTER = 1;

    private final TurnAlertPolicy defaultLimits = new TurnAlertPolicy(1, 3, 6);

    @Test
    void isTurnNear_nearWithOnePersonAhead_returnsTrue() {
        assertTrue(defaultLimits.isTurnNear(1, TicketDistance.NEAR, ONE_OPEN_COUNTER));
    }

    @Test
    void isTurnNear_nearWithTwoPeopleAhead_returnsFalse() {
        assertFalse(defaultLimits.isTurnNear(2, TicketDistance.NEAR, ONE_OPEN_COUNTER));
    }

    @Test
    void isTurnNear_mediumWithThreePeopleAhead_returnsTrue() {
        assertTrue(defaultLimits.isTurnNear(3, TicketDistance.MEDIUM, ONE_OPEN_COUNTER));
    }

    @Test
    void isTurnNear_mediumWithFourPeopleAhead_returnsFalse() {
        assertFalse(defaultLimits.isTurnNear(4, TicketDistance.MEDIUM, ONE_OPEN_COUNTER));
    }

    @Test
    void isTurnNear_farWithSixPeopleAhead_returnsTrue() {
        assertTrue(defaultLimits.isTurnNear(6, TicketDistance.FAR, ONE_OPEN_COUNTER));
    }

    @Test
    void isTurnNear_farWithSevenPeopleAhead_returnsFalse() {
        assertFalse(defaultLimits.isTurnNear(7, TicketDistance.FAR, ONE_OPEN_COUNTER));
    }

    @Test
    void isTurnNear_withNoPeopleAhead_returnsTrueForEveryDistance() {
        for (TicketDistance distance : TicketDistance.values()) {
            assertTrue(defaultLimits.isTurnNear(0, distance, ONE_OPEN_COUNTER));
        }
    }

    @Test
    void isTurnNear_withFourPeopleAhead_alertsFarButNotNear() {
        assertTrue(defaultLimits.isTurnNear(4, TicketDistance.FAR, ONE_OPEN_COUNTER));
        assertFalse(defaultLimits.isTurnNear(4, TicketDistance.NEAR, ONE_OPEN_COUNTER));
    }

    @Test
    void isTurnNear_withConfiguredLimits_usesConfiguredNumbers() {
        TurnAlertPolicy configuredLimits = new TurnAlertPolicy(0, 2, 10);

        assertFalse(configuredLimits.isTurnNear(1, TicketDistance.NEAR, ONE_OPEN_COUNTER));
        assertTrue(configuredLimits.isTurnNear(2, TicketDistance.MEDIUM, ONE_OPEN_COUNTER));
        assertTrue(configuredLimits.isTurnNear(10, TicketDistance.FAR, ONE_OPEN_COUNTER));
        assertFalse(configuredLimits.isTurnNear(11, TicketDistance.FAR, ONE_OPEN_COUNTER));
    }

    @Test
    void peopleAheadLimitFor_everyDistance_returnsItsConfiguredLimit() {
        assertEquals(1, defaultLimits.peopleAheadLimitFor(TicketDistance.NEAR));
        assertEquals(3, defaultLimits.peopleAheadLimitFor(TicketDistance.MEDIUM));
        assertEquals(6, defaultLimits.peopleAheadLimitFor(TicketDistance.FAR));
    }

    @Test
    void alertAtPeopleAheadFor_withOneOpenCounter_returnsTheLimit() {
        assertEquals(1, defaultLimits.alertAtPeopleAheadFor(TicketDistance.NEAR, 1));
        assertEquals(3, defaultLimits.alertAtPeopleAheadFor(TicketDistance.MEDIUM, 1));
        assertEquals(6, defaultLimits.alertAtPeopleAheadFor(TicketDistance.FAR, 1));
    }

    @Test
    void alertAtPeopleAheadFor_withTwoOpenCounters_returnsLimitTimesTwo() {
        assertEquals(2, defaultLimits.alertAtPeopleAheadFor(TicketDistance.NEAR, 2));
        assertEquals(6, defaultLimits.alertAtPeopleAheadFor(TicketDistance.MEDIUM, 2));
        assertEquals(12, defaultLimits.alertAtPeopleAheadFor(TicketDistance.FAR, 2));
    }

    @Test
    void alertAtPeopleAheadFor_withThreeOpenCounters_returnsLimitTimesThree() {
        assertEquals(3, defaultLimits.alertAtPeopleAheadFor(TicketDistance.NEAR, 3));
        assertEquals(9, defaultLimits.alertAtPeopleAheadFor(TicketDistance.MEDIUM, 3));
        assertEquals(18, defaultLimits.alertAtPeopleAheadFor(TicketDistance.FAR, 3));
    }

    @Test
    void alertAtPeopleAheadFor_withZeroLimit_staysZeroForAnyCount() {
        TurnAlertPolicy nextOnlyNear = new TurnAlertPolicy(0, 2, 10);

        for (int openCounterCount = 1; openCounterCount <= 3; openCounterCount++) {
            assertEquals(0, nextOnlyNear.alertAtPeopleAheadFor(TicketDistance.NEAR, openCounterCount));
        }
    }

    @Test
    void alertAtPeopleAheadFor_withZeroOpenCounters_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> defaultLimits.alertAtPeopleAheadFor(TicketDistance.MEDIUM, 0));
    }

    @Test
    void isTurnNear_withTwoOpenCountersAndTwiceTheLimitAhead_returnsTrue() {
        assertTrue(defaultLimits.isTurnNear(6, TicketDistance.MEDIUM, 2));
    }

    @Test
    void isTurnNear_withTwoOpenCountersAndOneMoreThanTwiceTheLimit_returnsFalse() {
        assertFalse(defaultLimits.isTurnNear(7, TicketDistance.MEDIUM, 2));
    }

    @Test
    void isTurnNear_withFiveAheadAndMediumDistance_dependsOnOpenCounters() {
        assertFalse(defaultLimits.isTurnNear(5, TicketDistance.MEDIUM, 1));
        assertTrue(defaultLimits.isTurnNear(5, TicketDistance.MEDIUM, 2));
        assertTrue(defaultLimits.isTurnNear(5, TicketDistance.MEDIUM, 3));
    }

    @Test
    void isTurnNear_withZeroOpenCounters_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> defaultLimits.isTurnNear(0, TicketDistance.NEAR, 0));
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
