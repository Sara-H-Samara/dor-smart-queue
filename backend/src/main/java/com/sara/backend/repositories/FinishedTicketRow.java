package com.sara.backend.repositories;

import java.time.Duration;
import java.time.LocalDateTime;

public record FinishedTicketRow(LocalDateTime calledAt, LocalDateTime finishedAt) {

    public Duration serviceDuration() {
        return Duration.between(calledAt, finishedAt);
    }
}
