package com.salon.app.module.booking.scheduler;

import com.salon.app.module.booking.entity.Booking;
import com.salon.app.module.booking.repository.BookingRepository;
import com.salon.app.module.booking.service.SlotAvailabilityService;
import com.salon.app.module.booking.service.SlotLockService;
import com.salon.app.module.coupon.service.CouponService;
import com.salon.app.shared.enums.BookingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCleanupScheduler {

    private final BookingRepository bookingRepository;
    private final CouponService couponService;
    private final SlotLockService slotLockService;
    private final SlotAvailabilityService slotAvailabilityService;

    @Scheduled(fixedDelay = 300000) // every 5 minutes
    @Transactional
    public void cleanupExpiredLockedBookings() {
        Instant cutoff = Instant.now().minusSeconds(660); // 11 minutes ago
        List<Booking> expiredBookings = bookingRepository.findExpiredLockedBookings(cutoff);
        if (expiredBookings.isEmpty()) {
            return;
        }

        log.info("Cleaning up {} expired SLOT_LOCKED bookings", expiredBookings.size());
        List<CleanupSideEffect> sideEffects = new ArrayList<>(expiredBookings.size());
        expiredBookings.forEach(booking -> {
            couponService.release(booking.getId(), "Booking slot lock expired");
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            sideEffects.add(new CleanupSideEffect(
                    booking.getOutlet().getId(),
                    booking.getScheduledDate(),
                    booking.getScheduledTime(),
                    booking.getStaff() == null ? null : booking.getStaff().getId(),
                    booking.getBookingRef()));
        });
        runAfterCommit(sideEffects);
    }

    private void runAfterCommit(List<CleanupSideEffect> sideEffects) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            applySideEffects(sideEffects);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                applySideEffects(sideEffects);
            }
        });
    }

    private void applySideEffects(List<CleanupSideEffect> sideEffects) {
        sideEffects.forEach(sideEffect -> {
            if (sideEffect.staffId() != null) {
                try {
                    slotLockService.releaseLock(sideEffect.outletId(), sideEffect.date(),
                            sideEffect.time(), sideEffect.staffId(), sideEffect.lockOwnerToken());
                } catch (RuntimeException ex) {
                    log.error("Failed to release expired slot lock for outlet {} on {}",
                            sideEffect.outletId(), sideEffect.date(), ex);
                }
            }
            try {
                slotAvailabilityService.broadcastSlotUpdate(
                        sideEffect.outletId(), sideEffect.date());
            } catch (RuntimeException ex) {
                log.error("Failed to broadcast cleanup for outlet {} on {}",
                        sideEffect.outletId(), sideEffect.date(), ex);
            }
        });
    }

    private record CleanupSideEffect(
            UUID outletId, LocalDate date, LocalTime time, UUID staffId, String lockOwnerToken) {
    }
}
