package com.sara.backend.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RoleChangeRequest(
        @NotBlank @Pattern(regexp = "STUDENT|STAFF|ADMIN") String role
) {}
