package com.sara.backend.dto.request;

import jakarta.validation.constraints.NotNull;

public record SetActiveRequest(
        @NotNull Boolean active
) {}
