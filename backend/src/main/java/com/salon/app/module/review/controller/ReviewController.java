package com.salon.app.module.review.controller;

import com.salon.app.module.auth.repository.UserRepository;
import com.salon.app.module.review.dto.request.CreateReviewRequest;
import com.salon.app.module.review.dto.response.ReviewResponse;
import com.salon.app.module.review.service.ReviewService;
import com.salon.app.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted", reviewService.createReview(resolveUserId(userDetails), request)));
    }

    @GetMapping("/staff/{staffId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getStaffReviews(@PathVariable UUID staffId) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getStaffReviews(staffId)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getMyReviews(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getMyReviews(resolveUserId(userDetails))));
    }

    private UUID resolveUserId(UserDetails userDetails) {
        return userRepository.findByPhoneNumberAndIsDeletedFalse(userDetails.getUsername())
                .orElseThrow().getId();
    }
}
