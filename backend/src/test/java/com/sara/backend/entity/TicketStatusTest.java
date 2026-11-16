package com.sara.backend.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketStatusTest {

    @Test
    void canChangeTo_fromWaitingToCalled_returnsTrue() {
        assertTrue(TicketStatus.WAITING.canChangeTo(TicketStatus.CALLED));
    }

    @Test
    void canChangeTo_fromWaitingToCancelled_returnsTrue() {
        assertTrue(TicketStatus.WAITING.canChangeTo(TicketStatus.CANCELLED));
    }

    @Test
    void canChangeTo_fromCalledToDone_returnsTrue() {
        assertTrue(TicketStatus.CALLED.canChangeTo(TicketStatus.DONE));
    }

    @Test
    void canChangeTo_fromCalledToNoShow_returnsTrue() {
        assertTrue(TicketStatus.CALLED.canChangeTo(TicketStatus.NO_SHOW));
    }

    @Test
    void canChangeTo_fromWaitingToDone_returnsFalse() {
        assertFalse(TicketStatus.WAITING.canChangeTo(TicketStatus.DONE));
    }

    @Test
    void canChangeTo_fromWaitingToNoShow_returnsFalse() {
        assertFalse(TicketStatus.WAITING.canChangeTo(TicketStatus.NO_SHOW));
    }

    @Test
    void canChangeTo_fromWaitingToWaiting_returnsFalse() {
        assertFalse(TicketStatus.WAITING.canChangeTo(TicketStatus.WAITING));
    }

    @Test
    void canChangeTo_fromCalledToWaiting_returnsFalse() {
        assertFalse(TicketStatus.CALLED.canChangeTo(TicketStatus.WAITING));
    }

    @Test
    void canChangeTo_fromCalledToCancelled_returnsFalse() {
        assertFalse(TicketStatus.CALLED.canChangeTo(TicketStatus.CANCELLED));
    }

    @Test
    void canChangeTo_fromCalledToCalled_returnsFalse() {
        assertFalse(TicketStatus.CALLED.canChangeTo(TicketStatus.CALLED));
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"DONE", "NO_SHOW", "CANCELLED"})
    void canChangeTo_fromFinishedStatus_returnsFalse(TicketStatus finishedStatus) {
        for (TicketStatus newStatus : TicketStatus.values()) {
            assertFalse(finishedStatus.canChangeTo(newStatus));
        }
    }
}
