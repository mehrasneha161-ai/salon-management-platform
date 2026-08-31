package com.salon.app.module.coupon.repository;

import com.salon.app.module.coupon.entity.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    List<Coupon> findByIsDeletedFalseOrderByCreatedAtDesc();

    Optional<Coupon> findByNormalizedCodeAndIsDeletedFalse(String normalizedCode);

    boolean existsByNormalizedCode(String normalizedCode);

    boolean existsByNormalizedCodeAndIdNot(String normalizedCode, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.id = :id AND c.isDeleted = false")
    Optional<Coupon> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.normalizedCode = :normalizedCode AND c.isDeleted = false")
    Optional<Coupon> findByNormalizedCodeForUpdate(@Param("normalizedCode") String normalizedCode);
}
