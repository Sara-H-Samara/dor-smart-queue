package com.sara.backend.dtos.responses;

import com.sara.backend.entities.Role;
import com.sara.backend.entities.User;

public record UserResponse(Long id, String fullName, String email, Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}