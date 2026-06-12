package com.salon.app.module.outlet.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OutletRequest {
    @NotBlank(message = "Outlet name is required")
    private String name;
    @NotBlank(message = "Address is required")
    private String address;
    private String city;
    private String phone;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private boolean isActive = true;
}
