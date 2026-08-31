package com.salon.app.module.review.service;

import com.salon.app.module.booking.entity.Booking;
import com.salon.app.module.booking.repository.BookingRepository;
import com.salon.app.module.review.dto.request.CreateReviewRequest;
import com.salon.app.module.review.dto.response.ReviewResponse;
import com.salon.app.module.review.entity.Review;
import com.salon.app.module.review.repository.ReviewRepository;
import com.salon.app.shared.enums.BookingStatus;
import com.salon.app.shared.exception.BusinessException;
import com.salon.app.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public ReviewResponse createReview(UUID customerId, CreateReviewRequest request) {
        log.info("Creating review for booking: {}", request.getBookingId());
        Booking booking = bookingRepository.findById(request.getBookingId())
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", request.getBookingId()));
        if (!booking.getCustomer().getId().equals(customerId)) {
            throw new BusinessException("You can only review your own bookings");
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BusinessException("You can only review completed bookings");
        }
        if (reviewRepository.existsByBookingId(booking.getId())) {
            throw new BusinessException("This booking has already been reviewed");
        }
        Review review = Review.builder()
                .booking(booking)
                .customer(booking.getCustomer())
                .staff(booking.getStaff())
                .rating((short) request.getRating())
                .comment(request.getComment())
                .build();
        return toResponse(reviewRepository.save(review));
    }

    @Override
    public List<ReviewResponse> getStaffReviews(UUID staffId) {
        return reviewRepository.findByStaffIdAndIsDeletedFalseOrderByCreatedAtDesc(staffId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<ReviewResponse> getMyReviews(UUID customerId) {
        return reviewRepository.findByCustomerIdAndIsDeletedFalseOrderByCreatedAtDesc(customerId)
                .stream().map(this::toResponse).toList();
    }

    private ReviewResponse toResponse(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .bookingId(r.getBooking().getId())
                .bookingRef(r.getBooking().getBookingRef())
                .customerId(r.getCustomer().getId())
                .customerName(r.getCustomer().getFullName())
                .staffId(r.getStaff() != null ? r.getStaff().getId() : null)
                .staffName(r.getStaff() != null ? r.getStaff().getUser().getFullName() : null)
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
