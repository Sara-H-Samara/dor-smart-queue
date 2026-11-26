package com.sara.backend.dtos.responses;

import com.sara.backend.entities.Role;
import com.sara.backend.entities.User;

public record UserRowResponse(Long id, String fullName, String email, Role role, boolean currentUser) {

    public static UserRowResponse from(User user, boolean currentUser) {
        return new UserRowResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(), currentUser);
    }
}
