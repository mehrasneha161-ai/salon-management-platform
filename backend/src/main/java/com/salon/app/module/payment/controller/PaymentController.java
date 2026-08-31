package com.salon.app.module.payment.controller;

import com.salon.app.module.auth.repository.UserRepository;
import com.salon.app.module.payment.dto.request.InitiatePaymentRequest;
import com.salon.app.module.payment.dto.request.VerifyPaymentRequest;
import com.salon.app.module.payment.dto.response.PaymentResponse;
import com.salon.app.module.payment.service.PaymentService;
import com.salon.app.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    @PostMapping("/initiate")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiate(
            @Valid @RequestBody InitiatePaymentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                "Payment initiated", paymentService.initiate(resolveUserId(userDetails), request)));
    }

    @PostMapping("/verify")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> verify(
            @Valid @RequestBody VerifyPaymentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                "Payment verified", paymentService.verify(resolveUserId(userDetails), request)));
    }

    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> getByBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getByBooking(bookingId)));
    }

    // Public (see SecurityConfig PUBLIC_ENDPOINTS: /api/v1/payments/webhook).
    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> webhook(@RequestBody Map<String, Object> payload) {
        paymentService.handleWebhook(payload);
        return ResponseEntity.ok(ApiResponse.success("Webhook processed", null));
    }

    private UUID resolveUserId(UserDetails userDetails) {
        return userRepository.findByPhoneNumberAndIsDeletedFalse(userDetails.getUsername())
                .orElseThrow().getId();
    }
}
