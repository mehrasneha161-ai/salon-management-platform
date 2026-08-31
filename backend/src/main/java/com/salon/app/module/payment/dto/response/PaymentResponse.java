package com.salon.app.module.payment.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class PaymentResponse {
    private UUID id;
    private UUID bookingId;
    private String bookingRef;
    private BigDecimal amount;
    private String currency;
    private String gateway;
    private String status;
    // Gateway order reference (used by the client checkout).
    private String orderRef;
    // Public gateway key id for the client checkout SDK (safe to expose).
    private String keyId;
    private Instant paidAt;
}
