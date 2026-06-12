package com.salon.app.module.service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PackageResponse {
    private UUID id;
    private String name;
    private String description;
    private UUID outletId;
    private BigDecimal price;
    private BigDecimal discountPct;
    private List<ServiceResponse> services;
    private boolean isActive;
}
