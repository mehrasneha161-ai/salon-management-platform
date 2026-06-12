package com.salon.app.module.service.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Data
public class PackageRequest {
    @NotBlank(message = "Package name is required")
    private String name;
    private String description;
    private UUID outletId;
    @NotNull @DecimalMin(value = "0.01")
    private BigDecimal price;
    private BigDecimal discountPct = BigDecimal.ZERO;
    private Set<UUID> serviceIds;
    private boolean isActive = true;
}
