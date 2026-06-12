package com.salon.app.module.auth.service;

import com.salon.app.module.auth.dto.request.LoginRequest;
import com.salon.app.module.auth.dto.request.RegisterRequest;
import com.salon.app.module.auth.dto.response.AuthResponse;
import com.salon.app.module.auth.entity.RefreshToken;
import com.salon.app.module.auth.entity.User;
import com.salon.app.module.auth.repository.RefreshTokenRepository;
import com.salon.app.module.auth.repository.UserRepository;
import com.salon.app.module.auth.security.JwtTokenProvider;
import com.salon.app.shared.enums.UserRole;
import com.salon.app.shared.exception.BusinessException;
import com.salon.app.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Value("${jwt.refresh-expiry-ms}")
    private long refreshExpiryMs;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new customer with phone: {}", request.getPhoneNumber());
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BusinessException("Phone number already registered");
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered");
        }
        User user = User.builder()
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .build();
        userRepository.save(user);
        log.info("Customer registered successfully: {}", user.getId());
        return generateAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for phone: {}", request.getPhoneNumber());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getPhoneNumber(), request.getPassword()));
        User user = userRepository.findByPhoneNumberAndIsDeletedFalse(request.getPhoneNumber())
                .orElseThrow(() -> new ResourceNotFoundException("User", "phoneNumber", request.getPhoneNumber()));
        return generateAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String token) {
        log.info("Refreshing token");
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndIsRevokedFalse(token)
                .orElseThrow(() -> new BusinessException("Invalid or expired refresh token"));
        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new BusinessException("Refresh token expired. Please login again.");
        }
        User user = refreshToken.getUser();
        return generateAuthResponse(user);
    }

    @Override
    @Transactional
    public void logout(String token) {
        log.info("Logging out user");
        refreshTokenRepository.findByTokenAndIsRevokedFalse(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    private AuthResponse generateAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getPhoneNumber());
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails, user.getRole().name());
        String refreshTokenValue = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiresAt(Instant.now().plusMillis(refreshExpiryMs))
                .isRevoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .role(user.getRole().name())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }
}
