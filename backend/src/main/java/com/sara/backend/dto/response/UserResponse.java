package com.sara.backend.dto.response;

import com.sara.backend.entity.Role;
import com.sara.backend.entity.User;

public record UserResponse(Long id, String fullName, String email, Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}