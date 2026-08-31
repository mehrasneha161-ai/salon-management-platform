package com.salon.app.module.review.repository;

import com.salon.app.module.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByStaffIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID staffId);
    List<Review> findByCustomerIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID customerId);
    boolean existsByBookingId(UUID bookingId);
}
