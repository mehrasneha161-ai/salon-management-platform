package com.salon.app.module.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String role;
    private String fullName;
    private String phoneNumber;
}
