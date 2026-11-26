package com.sara.backend.controllers;

import com.sara.backend.dtos.requests.RoleChangeRequest;
import com.sara.backend.dtos.responses.UserPageResponse;
import com.sara.backend.dtos.responses.UserRowResponse;
import com.sara.backend.services.UserRoleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class UserRoleController {

    @Autowired
    private UserRoleService userRoleService;

    @GetMapping
    public UserPageResponse listUsers(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return userRoleService.listUsers(search, page, size, authentication.getName());
    }

    @PatchMapping("/{id}/role")
    public UserRowResponse changeRole(
            @PathVariable Long id, @Valid @RequestBody RoleChangeRequest request, Authentication authentication) {
        return userRoleService.changeRole(id, request, authentication.getName());
    }
}
