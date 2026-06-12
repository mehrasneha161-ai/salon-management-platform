package com.salon.app.module.booking.scheduler;

import com.salon.app.module.booking.entity.Booking;
import com.salon.app.module.booking.repository.BookingRepository;
import com.salon.app.module.booking.service.SlotLockService;
import com.salon.app.shared.enums.BookingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCleanupScheduler {

    private final BookingRepository bookingRepository;

    @Scheduled(fixedDelay = 300000) // every 5 minutes
    @Transactional
    public void cleanupExpiredLockedBookings() {
        Instant cutoff = Instant.now().minusSeconds(660); // 11 minutes ago
        List<Booking> expiredBookings = bookingRepository.findExpiredLockedBookings(cutoff);
        if (!expiredBookings.isEmpty()) {
            log.info("Cleaning up {} expired SLOT_LOCKED bookings", expiredBookings.size());
            expiredBookings.forEach(booking -> {
                booking.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(booking);
            });
        }
    }
}
