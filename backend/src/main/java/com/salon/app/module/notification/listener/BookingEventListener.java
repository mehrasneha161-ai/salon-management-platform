package com.salon.app.module.notification.listener;

import com.salon.app.module.booking.event.BookingConfirmedEvent;
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

    @Async
    @EventListener
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        log.info("Handling BookingConfirmedEvent for: {}", event.getBooking().getBookingRef());
        notificationService.sendConfirmation(event.getBooking());
    }
}
