package com.salon.app.module.coupon.dto.response;

import com.salon.app.shared.enums.CouponDiscountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class CouponResponse {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private CouponDiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minimumSpend;
    private BigDecimal maximumDiscount;
    private Instant validFrom;
    private Instant validUntil;
    private Integer usageLimit;
    private Integer perCustomerLimit;
    private long reservedCount;
    private long redeemedCount;
    private UUID outletId;
    private UUID serviceId;
    private UUID packageId;
    private boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
