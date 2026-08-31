package com.salon.app.module.staff.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class LeaveResponse {
    private UUID id;
    private UUID staffId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
}
