package com.salon.app.module.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class OutletRevenueDto {
    private UUID outletId;
    private String outletName;
    private BigDecimal totalRevenue;
    private long totalBookings;
}
