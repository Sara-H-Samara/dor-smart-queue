package com.sara.backend.dtos.responses;

import com.sara.backend.entities.User;
import org.springframework.data.domain.Page;

import java.util.List;

public record UserPageResponse(List<UserRowResponse> users, int page, int size, long totalUsers, int totalPages) {

    public static UserPageResponse from(Page<User> userPage, String callerEmail) {
        List<UserRowResponse> users = userPage.getContent().stream()
                .map(user -> UserRowResponse.from(user, user.getEmail().equals(callerEmail)))
                .toList();
        return new UserPageResponse(
                users, userPage.getNumber(), userPage.getSize(), userPage.getTotalElements(), userPage.getTotalPages());
    }
}
