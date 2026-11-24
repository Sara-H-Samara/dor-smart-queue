package com.sara.backend.services;

import com.sara.backend.entities.TicketDistance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TurnAlertPolicy {

    private final int nearPeopleAheadLimit;
    private final int mediumPeopleAheadLimit;
    private final int farPeopleAheadLimit;

    public TurnAlertPolicy(
            @Value("${dor.turn-alert.near-people-ahead:1}") int nearPeopleAheadLimit,
            @Value("${dor.turn-alert.medium-people-ahead:3}") int mediumPeopleAheadLimit,
            @Value("${dor.turn-alert.far-people-ahead:6}") int farPeopleAheadLimit) {
        if (nearPeopleAheadLimit < 0
                || nearPeopleAheadLimit > mediumPeopleAheadLimit
                || mediumPeopleAheadLimit > farPeopleAheadLimit) {
            throw new IllegalArgumentException(
                    "Turn alert limits must not be negative and must grow from near to medium to far.");
        }
        this.nearPeopleAheadLimit = nearPeopleAheadLimit;
        this.mediumPeopleAheadLimit = mediumPeopleAheadLimit;
        this.farPeopleAheadLimit = farPeopleAheadLimit;
    }

    public int peopleAheadLimitFor(TicketDistance distance) {
        return switch (distance) {
            case NEAR -> nearPeopleAheadLimit;
            case MEDIUM -> mediumPeopleAheadLimit;
            case FAR -> farPeopleAheadLimit;
        };
    }

    public boolean isTurnNear(int peopleAhead, TicketDistance distance) {
        return peopleAhead <= peopleAheadLimitFor(distance);
    }
}
