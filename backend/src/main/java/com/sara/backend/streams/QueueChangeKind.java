package com.sara.backend.streams;

public enum QueueChangeKind {
    TICKET_CHANGED("ticket-changed"),
    CONFIGURATION_CHANGED("configuration-changed");

    private final String eventName;

    QueueChangeKind(String eventName) {
        this.eventName = eventName;
    }

    public String eventName() {
        return eventName;
    }
}
