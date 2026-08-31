package com.salon.app.module.booking.scheduler;

import com.salon.app.module.booking.entity.Booking;
import com.salon.app.module.booking.event.BookingReminderEvent;
import com.salon.app.module.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Sends a one-time reminder (WhatsApp + email) the day before each confirmed
 * booking. Runs hourly; the {@code reminderSent} flag guarantees at-most-once.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingReminderScheduler {

    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedDelay = 3600000) // hourly
    @Transactional
    public void sendUpcomingReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Booking> due = bookingRepository.findConfirmedForReminder(tomorrow);
        if (due.isEmpty()) {
            return;
        }
        log.info("Queueing reminders for {} booking(s) scheduled on {}", due.size(), tomorrow);
        for (Booking booking : due) {
            eventPublisher.publishEvent(new BookingReminderEvent(this, booking));
            booking.setReminderSent(true);
        }
        bookingRepository.saveAll(due);
    }
}
