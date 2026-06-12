package com.salon.app.module.service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class ServiceResponse {
    private UUID id;
    private String name;
    private String description;
    private String categoryName;
    private UUID categoryId;
    private UUID outletId;
    private int durationMinutes;
    private BigDecimal price;
    private boolean isActive;
}
