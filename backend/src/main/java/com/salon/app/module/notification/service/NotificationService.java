package com.salon.app.module.notification.service;

import com.salon.app.module.booking.entity.Booking;
import com.salon.app.module.notification.dto.BroadcastRequest;

import java.util.List;

public interface NotificationService {
    void sendConfirmation(Booking booking);
    void sendReminder(Booking booking);
    void sendBroadcast(BroadcastRequest request);
    void sendMarketingCampaign(String message, List<String> phoneNumbers);
}
