package com.salon.app.module.review.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ReviewResponse {
    private UUID id;
    private UUID bookingId;
    private String bookingRef;
    private UUID customerId;
    private String customerName;
    private UUID staffId;
    private String staffName;
    private int rating;
    private String comment;
    private Instant createdAt;
}
