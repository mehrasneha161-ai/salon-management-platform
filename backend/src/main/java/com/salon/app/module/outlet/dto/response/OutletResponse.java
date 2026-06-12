package com.salon.app.module.outlet.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class OutletResponse {
    private UUID id;
    private String name;
    private String address;
    private String city;
    private String phone;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private boolean isActive;
    private Instant createdAt;
}
