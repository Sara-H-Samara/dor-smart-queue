package com.sara.backend.controllers;

import com.sara.backend.services.AuthService;
import com.sara.backend.dtos.requests.LoginRequest;
import com.sara.backend.dtos.responses.LoginResponse;
import com.sara.backend.dtos.requests.RegisterRequest;
import com.sara.backend.dtos.responses.UserResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
}