package com.salon.app.module.notification.service;

import com.salon.app.module.booking.entity.Booking;
import com.salon.app.module.notification.dto.BroadcastRequest;
import com.salon.app.module.notification.entity.WhatsAppNotification;
import com.salon.app.module.notification.repository.WhatsAppNotificationRepository;
import com.salon.app.shared.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppNotificationServiceImpl implements NotificationService {

    private final WhatsAppNotificationRepository notificationRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${whatsapp.api.url}")
    private String apiUrl;

    @Value("${whatsapp.api.token}")
    private String apiToken;

    @Value("${whatsapp.api.phone-number-id}")
    private String phoneNumberId;

    @Override
    @Async
    public void sendConfirmation(Booking booking) {
        log.info("Sending booking confirmation to: {}", booking.getCustomer().getPhoneNumber());
        Map<String, Object> payload = buildConfirmationPayload(booking);
        sendWhatsAppMessage(booking.getCustomer().getPhoneNumber(),
                "booking_confirmation", payload, NotificationType.CONFIRMATION);
    }

    @Override
    @Async
    public void sendReminder(Booking booking) {
        log.info("Sending reminder to: {}", booking.getCustomer().getPhoneNumber());
        Map<String, Object> payload = new HashMap<>();
        payload.put("customerName", booking.getCustomer().getFullName());
        payload.put("date", booking.getScheduledDate().toString());
        payload.put("time", booking.getScheduledTime().toString());
        sendWhatsAppMessage(booking.getCustomer().getPhoneNumber(),
                "booking_reminder", payload, NotificationType.REMINDER);
    }

    @Override
    @Async
    public void sendBroadcast(BroadcastRequest request) {
        log.info("Sending broadcast to {} recipients", request.getPhoneNumbers().size());
        request.getPhoneNumbers().forEach(phone ->
                sendWhatsAppMessage(phone, null, Map.of("message", request.getMessage()), NotificationType.BROADCAST));
    }

    @Override
    @Async
    public void sendMarketingCampaign(String message, List<String> phoneNumbers) {
        log.info("Sending marketing campaign to {} recipients", phoneNumbers.size());
        phoneNumbers.forEach(phone ->
                sendWhatsAppMessage(phone, null, Map.of("message", message), NotificationType.MARKETING));
    }

    private void sendWhatsAppMessage(String phone, String templateName,
                                      Map<String, Object> payload, NotificationType type) {
        WhatsAppNotification notification = WhatsAppNotification.builder()
                .recipientPhone(phone)
                .messageType(type)
                .templateName(templateName)
                .payload(payload)
                .status("QUEUED")
                .build();
        notificationRepository.save(notification);
        try {
            Map<String, Object> requestBody = buildWhatsAppRequestBody(phone, templateName, payload);
            webClientBuilder.build()
                    .post()
                    .uri(apiUrl + "/" + phoneNumberId + "/messages")
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe(
                            response -> {
                                notification.setStatus("SENT");
                                notification.setSentAt(Instant.now());
                                notificationRepository.save(notification);
                            },
                            error -> {
                                notification.setStatus("FAILED");
                                notification.setErrorMessage(error.getMessage());
                                notificationRepository.save(notification);
                                log.error("WhatsApp send failed for {}: {}", phone, error.getMessage());
                            }
                    );
        } catch (Exception ex) {
            notification.setStatus("FAILED");
            notification.setErrorMessage(ex.getMessage());
            notificationRepository.save(notification);
        }
    }

    private Map<String, Object> buildWhatsAppRequestBody(String phone, String templateName, Map<String, Object> payload) {
        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", "91" + phone);
        if (templateName != null) {
            body.put("type", "template");
            body.put("template", Map.of("name", templateName, "language", Map.of("code", "en")));
        } else {
            body.put("type", "text");
            body.put("text", Map.of("body", payload.getOrDefault("message", "")));
        }
        return body;
    }

    private Map<String, Object> buildConfirmationPayload(Booking booking) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("customerName", booking.getCustomer().getFullName());
        payload.put("bookingRef", booking.getBookingRef());
        payload.put("outletName", booking.getOutlet().getName());
        payload.put("date", booking.getScheduledDate().toString());
        payload.put("time", booking.getScheduledTime().toString());
        payload.put("amount", booking.getTotalAmount().toString());
        return payload;
    }
}
