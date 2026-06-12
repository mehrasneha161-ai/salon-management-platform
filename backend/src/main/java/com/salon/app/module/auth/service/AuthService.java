package com.salon.app.module.auth.service;

import com.salon.app.module.auth.dto.request.LoginRequest;
import com.salon.app.module.auth.dto.request.RegisterRequest;
import com.salon.app.module.auth.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(String refreshToken);
    void logout(String refreshToken);
}
