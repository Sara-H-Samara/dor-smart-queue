package com.sara.backend.dto.request;

import jakarta.validation.constraints.NotNull;

public record TakeTicketRequest(
        @NotNull Long serviceId
) {}
