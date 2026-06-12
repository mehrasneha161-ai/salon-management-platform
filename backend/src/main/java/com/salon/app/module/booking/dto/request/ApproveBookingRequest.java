package com.salon.app.module.booking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ApproveBookingRequest {
    @NotNull(message = "Staff assignment is required")
    private UUID staffId;
}
