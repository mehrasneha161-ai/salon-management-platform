package com.salon.app.module.coupon.dto.request;

import com.salon.app.shared.enums.CouponDiscountType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class CouponRequest {

    @NotBlank(message = "Coupon code is required")
    @Size(max = 50, message = "Coupon code must not exceed 50 characters")
    private String code;

    @NotBlank(message = "Coupon name is required")
    @Size(max = 150, message = "Coupon name must not exceed 150 characters")
    private String name;

    private String description;

    @NotNull(message = "Discount type is required")
    private CouponDiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be positive")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal discountValue;

    @DecimalMin(value = "0.00", message = "Minimum spend cannot be negative")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal minimumSpend = BigDecimal.ZERO;

    @DecimalMin(value = "0.01", message = "Maximum discount must be positive")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal maximumDiscount;

    @NotNull(message = "Valid from is required")
    private Instant validFrom;

    @NotNull(message = "Valid until is required")
    private Instant validUntil;

    @Min(value = 1, message = "Usage limit must be positive")
    private Integer usageLimit;

    @Min(value = 1, message = "Per-customer limit must be positive")
    private Integer perCustomerLimit;

    private UUID outletId;
    private UUID serviceId;
    private UUID packageId;
    private boolean isActive = true;
}
