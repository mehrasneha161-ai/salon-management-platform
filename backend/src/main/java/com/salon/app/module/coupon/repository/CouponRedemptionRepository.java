package com.salon.app.module.coupon.repository;

import com.salon.app.module.coupon.entity.CouponRedemption;
import com.salon.app.shared.enums.CouponRedemptionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, UUID> {

    Optional<CouponRedemption> findByBookingIdAndIsDeletedFalse(UUID bookingId);

    long countByCouponIdAndStatusAndIsDeletedFalse(
            UUID couponId, CouponRedemptionStatus status);

    long countByCouponIdAndStatusInAndIsDeletedFalse(
            UUID couponId, Collection<CouponRedemptionStatus> statuses);

    long countByCouponIdAndCustomerIdAndStatusInAndIsDeletedFalse(
            UUID couponId, UUID customerId, Collection<CouponRedemptionStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM CouponRedemption r WHERE r.booking.id = :bookingId AND r.isDeleted = false")
    Optional<CouponRedemption> findByBookingIdForUpdate(@Param("bookingId") UUID bookingId);
}
