package com.sara.backend.services;

import com.sara.backend.dtos.requests.LoginRequest;
import com.sara.backend.dtos.requests.RegisterRequest;
import com.sara.backend.dtos.responses.LoginResponse;
import com.sara.backend.dtos.responses.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    UserResponse me(String email);
}
