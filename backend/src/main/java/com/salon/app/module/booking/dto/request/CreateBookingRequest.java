package com.salon.app.module.booking.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
public class CreateBookingRequest {
    @NotNull(message = "Outlet is required")
    private UUID outletId;
    @NotNull(message = "Staff is required")
    private UUID staffId;
    private UUID serviceId;
    private UUID packageId;
    @NotNull @Future
    private LocalDate scheduledDate;
    @NotNull
    private LocalTime scheduledTime;
    private String notes;
    private String sessionId;
}
