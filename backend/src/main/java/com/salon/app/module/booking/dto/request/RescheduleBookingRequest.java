package com.salon.app.module.booking.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
public class RescheduleBookingRequest {
    @NotNull @Future
    private LocalDate scheduledDate;
    @NotNull
    private LocalTime scheduledTime;
    // Optional: move to a different stylist. If null, the current stylist is kept.
    private UUID staffId;
    private String sessionId;
}
