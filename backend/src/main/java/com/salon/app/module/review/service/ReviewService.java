package com.salon.app.module.review.service;

import com.salon.app.module.review.dto.request.CreateReviewRequest;
import com.salon.app.module.review.dto.response.ReviewResponse;

import java.util.List;
import java.util.UUID;

public interface ReviewService {
    ReviewResponse createReview(UUID customerId, CreateReviewRequest request);
    List<ReviewResponse> getStaffReviews(UUID staffId);
    List<ReviewResponse> getMyReviews(UUID customerId);
}
