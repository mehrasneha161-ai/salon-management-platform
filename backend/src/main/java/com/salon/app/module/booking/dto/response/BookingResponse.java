package com.salon.app.module.booking.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
public class BookingResponse {
    private UUID id;
    private String bookingRef;
    private String customerName;
    private String customerPhone;
    private UUID outletId;
    private String outletName;
    private UUID staffId;
    private String staffName;
    private String serviceName;
    private String packageName;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private int durationMinutes;
    private String status;
    private BigDecimal totalAmount;
    private String notes;
    private Instant createdAt;
}
