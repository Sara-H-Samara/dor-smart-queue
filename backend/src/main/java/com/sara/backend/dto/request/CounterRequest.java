package com.sara.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CounterRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Long serviceId
) {}
