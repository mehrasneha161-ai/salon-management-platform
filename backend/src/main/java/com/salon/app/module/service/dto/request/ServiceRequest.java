package com.salon.app.module.service.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ServiceRequest {
    @NotBlank(message = "Service name is required")
    private String name;
    private String description;
    @NotNull(message = "Category is required")
    private UUID categoryId;
    private UUID outletId;
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private int durationMinutes;
    @NotNull @DecimalMin(value = "0.01", message = "Price must be positive")
    private BigDecimal price;
    private boolean isActive = true;
}
