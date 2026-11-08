package com.sara.backend.controller;

import com.sara.backend.service.AuthService;
import com.sara.backend.dto.request.LoginRequest;
import com.sara.backend.dto.response.LoginResponse;
import com.sara.backend.dto.request.RegisterRequest;
import com.sara.backend.dto.response.UserResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return authService.me(authentication.getName());
    }
}