package com.salon.app.module.staff.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class AttendanceResponse {
    private UUID id;
    private LocalDate date;
    private Instant checkInAt;
    private Instant checkOutAt;
    private String status;
}
