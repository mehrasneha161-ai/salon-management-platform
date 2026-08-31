package com.salon.app.module.coupon.dto.response;

import com.salon.app.shared.enums.CouponDiscountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable, trusted coupon pricing snapshot prepared for booking integration.
 */
public record CouponApplication(
        UUID couponId,
        String couponCode,
        CouponDiscountType discountType,
        BigDecimal discountValue,
        BigDecimal maximumDiscount,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        UUID outletId,
        UUID serviceId,
        UUID packageId,
        Instant preparedAt
) {
}
