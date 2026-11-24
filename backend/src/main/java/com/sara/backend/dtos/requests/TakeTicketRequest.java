package com.sara.backend.dtos.requests;

import com.sara.backend.entities.TicketDistance;
import jakarta.validation.constraints.NotNull;

public record TakeTicketRequest(
        @NotNull Long serviceId,
        TicketDistance distance
) {}
