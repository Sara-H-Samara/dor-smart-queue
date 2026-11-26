package com.sara.backend.entities;

import java.util.List;

public enum TicketStatus {
    WAITING,
    CALLED,
    DONE,
    NO_SHOW,
    CANCELLED;

    public static final List<TicketStatus> ACTIVE_STATUSES = List.of(WAITING, CALLED);

    public boolean canChangeTo(TicketStatus newStatus) {
        return switch (this) {
            case WAITING -> newStatus == CALLED || newStatus == CANCELLED;
            case CALLED -> newStatus == DONE || newStatus == NO_SHOW;
            case DONE, NO_SHOW, CANCELLED -> false;
        };
    }
}
