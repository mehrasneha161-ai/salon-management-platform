package com.salon.app.module.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class VerifyPaymentRequest {
    @NotNull(message = "paymentId is required")
    private UUID paymentId;
    // Values returned by the gateway checkout on the client.
    private String gatewayPaymentId;
    private String gatewaySignature;
}
