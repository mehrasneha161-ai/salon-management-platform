package com.salon.app.module.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PopularServiceDto {
    private UUID serviceId;
    private String serviceName;
    private long bookingCount;
    private String categoryName;
}
