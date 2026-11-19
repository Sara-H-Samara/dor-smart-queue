package com.sara.backend.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Ticket status changes")
class TicketStatusTest {

    @Test
    @DisplayName("A waiting ticket can be called")
    void canChangeTo_fromWaitingToCalled_returnsTrue() {
        assertTrue(TicketStatus.WAITING.canChangeTo(TicketStatus.CALLED));
    }

    @Test
    @DisplayName("A waiting ticket can be cancelled")
    void canChangeTo_fromWaitingToCancelled_returnsTrue() {
        assertTrue(TicketStatus.WAITING.canChangeTo(TicketStatus.CANCELLED));
    }

    @Test
    @DisplayName("A called ticket can be marked done")
    void canChangeTo_fromCalledToDone_returnsTrue() {
        assertTrue(TicketStatus.CALLED.canChangeTo(TicketStatus.DONE));
    }

    @Test
    @DisplayName("A called ticket can be marked as no show")
    void canChangeTo_fromCalledToNoShow_returnsTrue() {
        assertTrue(TicketStatus.CALLED.canChangeTo(TicketStatus.NO_SHOW));
    }

    @Test
    @DisplayName("A waiting ticket cannot be marked done before it is called")
    void canChangeTo_fromWaitingToDone_returnsFalse() {
        assertFalse(TicketStatus.WAITING.canChangeTo(TicketStatus.DONE));
    }

    @Test
    @DisplayName("A waiting ticket cannot be marked as no show before it is called")
    void canChangeTo_fromWaitingToNoShow_returnsFalse() {
        assertFalse(TicketStatus.WAITING.canChangeTo(TicketStatus.NO_SHOW));
    }

    @Test
    @DisplayName("A waiting ticket cannot change to waiting again")
    void canChangeTo_fromWaitingToWaiting_returnsFalse() {
        assertFalse(TicketStatus.WAITING.canChangeTo(TicketStatus.WAITING));
    }

    @Test
    @DisplayName("A called ticket cannot go back to waiting")
    void canChangeTo_fromCalledToWaiting_returnsFalse() {
        assertFalse(TicketStatus.CALLED.canChangeTo(TicketStatus.WAITING));
    }

    @Test
    @DisplayName("A called ticket cannot be cancelled")
    void canChangeTo_fromCalledToCancelled_returnsFalse() {
        assertFalse(TicketStatus.CALLED.canChangeTo(TicketStatus.CANCELLED));
    }

    @Test
    @DisplayName("A called ticket cannot be called again")
    void canChangeTo_fromCalledToCalled_returnsFalse() {
        assertFalse(TicketStatus.CALLED.canChangeTo(TicketStatus.CALLED));
    }

    @ParameterizedTest(name = "{0} cannot change to any status")
    @DisplayName("A finished ticket cannot change to any other status")
    @EnumSource(value = TicketStatus.class, names = {"DONE", "NO_SHOW", "CANCELLED"})
    void canChangeTo_fromFinishedStatus_returnsFalse(TicketStatus finishedStatus) {
        for (TicketStatus newStatus : TicketStatus.values()) {
            assertFalse(finishedStatus.canChangeTo(newStatus));
        }
    }
}
