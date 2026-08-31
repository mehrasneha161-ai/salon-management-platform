package com.salon.app.module.payment.service;

import com.salon.app.module.payment.dto.request.InitiatePaymentRequest;
import com.salon.app.module.payment.dto.request.VerifyPaymentRequest;
import com.salon.app.module.payment.dto.response.PaymentResponse;

import java.util.Map;
import java.util.UUID;

public interface PaymentService {
    PaymentResponse initiate(UUID customerId, InitiatePaymentRequest request);
    PaymentResponse verify(UUID customerId, VerifyPaymentRequest request);
    PaymentResponse getByBooking(UUID bookingId);
    void handleWebhook(Map<String, Object> payload);
}
