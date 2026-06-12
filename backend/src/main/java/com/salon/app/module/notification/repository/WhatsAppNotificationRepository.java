package com.salon.app.module.notification.repository;

import com.salon.app.module.notification.entity.WhatsAppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WhatsAppNotificationRepository extends JpaRepository<WhatsAppNotification, UUID> {
    List<WhatsAppNotification> findByStatus(String status);
    List<WhatsAppNotification> findByRecipientPhone(String phone);
}
