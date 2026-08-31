package com.salon.app.module.notification.service;

import com.salon.app.module.booking.entity.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Email notifications (via Spring Mail). Best-effort and asynchronous: failures
 * are logged, never propagated, and customers without an email are skipped.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    @Async
    public void sendBookingConfirmation(Booking booking) {
        String to = booking.getCustomer().getEmail();
        if (to == null || to.isBlank()) return;
        send(to, "Booking confirmed — " + booking.getBookingRef(),
                "Hi " + booking.getCustomer().getFullName() + ",\n\n"
                        + "Your appointment " + booking.getBookingRef() + " at "
                        + booking.getOutlet().getName() + " on " + booking.getScheduledDate()
                        + " at " + booking.getScheduledTime() + " is confirmed.\n\nThank you!");
    }

    @Async
    public void sendBookingReminder(Booking booking) {
        String to = booking.getCustomer().getEmail();
        if (to == null || to.isBlank()) return;
        send(to, "Reminder — your appointment tomorrow (" + booking.getBookingRef() + ")",
                "Hi " + booking.getCustomer().getFullName() + ",\n\n"
                        + "This is a reminder for your appointment " + booking.getBookingRef() + " at "
                        + booking.getOutlet().getName() + " on " + booking.getScheduledDate()
                        + " at " + booking.getScheduledTime() + ".\n\nSee you soon!");
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (from != null && !from.isBlank()) message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (Exception ex) {
            log.warn("Email send failed to {}: {}", to, ex.getMessage());
        }
    }
}
