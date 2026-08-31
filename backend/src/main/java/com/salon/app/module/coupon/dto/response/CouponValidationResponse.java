package com.salon.app.module.coupon.dto.response;

import com.salon.app.shared.enums.CouponDiscountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class CouponValidationResponse {
    private UUID couponId;
    private String code;
    private CouponDiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maximumDiscount;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private Instant validUntil;
}
