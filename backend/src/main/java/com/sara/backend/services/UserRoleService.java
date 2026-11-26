package com.sara.backend.services;

import com.sara.backend.dtos.requests.RoleChangeRequest;
import com.sara.backend.dtos.responses.UserPageResponse;
import com.sara.backend.dtos.responses.UserRowResponse;

public interface UserRoleService {

    UserPageResponse listUsers(String search, int page, int size, String callerEmail);

    UserRowResponse changeRole(Long userId, RoleChangeRequest request, String callerEmail);
}
