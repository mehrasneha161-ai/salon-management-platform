package com.salon.app.module.booking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
public class CreateBookingRequest {
    @NotNull(message = "Outlet is required")
    private UUID outletId;
    @NotNull(message = "Staff is required")
    private UUID staffId;
    private UUID serviceId;
    private UUID packageId;
    @Size(max = 50, message = "Coupon code must not exceed 50 characters")
    private String couponCode;
    private UUID expectedCouponId;
    @DecimalMin(value = "0.00", message = "Expected subtotal amount must be nonnegative")
    private BigDecimal expectedSubtotalAmount;
    @DecimalMin(value = "0.00", message = "Expected discount amount must be nonnegative")
    private BigDecimal expectedDiscountAmount;
    @DecimalMin(value = "0.00", message = "Expected total amount must be nonnegative")
    private BigDecimal expectedTotalAmount;
    @NotNull @Future
    private LocalDate scheduledDate;
    @NotNull
    private LocalTime scheduledTime;
    private String notes;
    private String sessionId;
}
