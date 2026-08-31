package com.salon.app.module.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateReviewRequest {
    @NotNull(message = "bookingId is required")
    private UUID bookingId;
    @Min(1) @Max(5)
    private int rating;
    private String comment;
}
