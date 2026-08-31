package com.salon.app.module.notification.listener;

import com.salon.app.module.booking.event.BookingConfirmedEvent;
import com.salon.app.module.booking.event.BookingReminderEvent;
import com.salon.app.module.notification.service.EmailService;
import com.salon.app.module.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventListener {

    private final NotificationService notificationService;
    private final EmailService emailService;

    @Async
    @EventListener
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        log.info("Handling BookingConfirmedEvent for: {}", event.getBooking().getBookingRef());
        notificationService.sendConfirmation(event.getBooking());
        emailService.sendBookingConfirmation(event.getBooking());
    }

    @Async
    @EventListener
    public void onBookingReminder(BookingReminderEvent event) {
        log.info("Handling BookingReminderEvent for: {}", event.getBooking().getBookingRef());
        notificationService.sendReminder(event.getBooking());
        emailService.sendBookingReminder(event.getBooking());
    }
}
