package com.salon.app.module.booking.repository;

import com.salon.app.module.booking.entity.Booking;
import com.salon.app.shared.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Page<Booking> findByCustomerIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.isDeleted = false " +
           "AND (:outletId IS NULL OR b.outlet.id = :outletId) " +
           "AND (:date IS NULL OR b.scheduledDate = :date) " +
           "AND (:status IS NULL OR b.status = :status)")
    Page<Booking> findByFilters(UUID outletId, LocalDate date, BookingStatus status, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.staff.id = :staffId " +
           "AND b.scheduledDate = :date " +
           "AND b.status IN ('CONFIRMED', 'IN_PROGRESS', 'SLOT_LOCKED') " +
           "AND b.isDeleted = false")
    List<Booking> findActiveBookingsForStaffOnDate(UUID staffId, LocalDate date);

    @Query("SELECT b FROM Booking b WHERE b.outlet.id = :outletId " +
           "AND b.scheduledDate = :date " +
           "AND b.scheduledTime = :time " +
           "AND b.status IN ('CONFIRMED', 'IN_PROGRESS', 'SLOT_LOCKED') " +
           "AND b.isDeleted = false")
    List<Booking> findConflictingBookings(UUID outletId, LocalDate date, LocalTime time);

    Optional<Booking> findByBookingRefAndIsDeletedFalse(String bookingRef);

    @Query("SELECT b FROM Booking b WHERE b.status = 'SLOT_LOCKED' " +
           "AND b.updatedAt < :cutoff")
    List<Booking> findExpiredLockedBookings(java.time.Instant cutoff);
}
