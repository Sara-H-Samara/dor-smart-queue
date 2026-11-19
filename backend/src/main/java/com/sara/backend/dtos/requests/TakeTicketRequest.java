package com.sara.backend.dtos.requests;

import jakarta.validation.constraints.NotNull;

public record TakeTicketRequest(
        @NotNull Long serviceId
) {}
