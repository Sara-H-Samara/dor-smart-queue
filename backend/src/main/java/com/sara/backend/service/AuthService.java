package com.sara.backend.service;

import com.sara.backend.dto.request.LoginRequest;
import com.sara.backend.dto.request.RegisterRequest;
import com.sara.backend.dto.response.LoginResponse;
import com.sara.backend.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    UserResponse me(String email);
}
