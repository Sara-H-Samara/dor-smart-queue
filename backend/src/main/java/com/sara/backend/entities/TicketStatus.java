package com.sara.backend.entities;

public enum TicketStatus {
    WAITING,
    CALLED,
    DONE,
    NO_SHOW,
    CANCELLED;

    public boolean canChangeTo(TicketStatus newStatus) {
        return switch (this) {
            case WAITING -> newStatus == CALLED || newStatus == CANCELLED;
            case CALLED -> newStatus == DONE || newStatus == NO_SHOW;
            case DONE, NO_SHOW, CANCELLED -> false;
        };
    }
}
