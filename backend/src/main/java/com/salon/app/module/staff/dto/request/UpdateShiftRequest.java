package com.salon.app.module.staff.dto.request;

import lombok.Data;

import java.time.LocalTime;

@Data
public class UpdateShiftRequest {
    // Both nullable: send null/null to clear the shift and follow outlet hours.
    private LocalTime shiftStart;
    private LocalTime shiftEnd;
}
