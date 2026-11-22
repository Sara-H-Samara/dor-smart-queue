package com.sara.backend.streams;

public enum StreamAudience {
    PUBLIC,
    STUDENT,
    STAFF,
    ADMIN;

    public boolean receives(QueueChangeKind kind) {
        return switch (this) {
            case PUBLIC, STUDENT, STAFF -> true;
            case ADMIN -> kind == QueueChangeKind.CONFIGURATION_CHANGED;
        };
    }
}
