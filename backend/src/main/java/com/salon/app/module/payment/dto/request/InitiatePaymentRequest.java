package com.salon.app.module.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class InitiatePaymentRequest {
    @NotNull(message = "bookingId is required")
    private UUID bookingId;
}
