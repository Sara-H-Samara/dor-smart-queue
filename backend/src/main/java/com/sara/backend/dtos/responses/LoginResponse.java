package com.sara.backend.dtos.responses;

import com.sara.backend.entities.Role;

public record LoginResponse(String token, String fullName, Role role) {}