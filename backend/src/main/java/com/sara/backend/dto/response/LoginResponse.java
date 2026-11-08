package com.sara.backend.dto.response;

import com.sara.backend.entity.Role;

public record LoginResponse(String token, String fullName, Role role) {}