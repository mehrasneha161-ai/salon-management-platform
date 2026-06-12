package com.salon.app.module.staff.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class StaffResponse {
    private UUID id;
    private UUID userId;
    private String fullName;
    private String phoneNumber;
    private String specialization;
    private String bio;
    private String profilePicUrl;
    private String status;
    private UUID outletId;
    private String outletName;
    private long totalPresentDays;
    private Instant createdAt;
}
