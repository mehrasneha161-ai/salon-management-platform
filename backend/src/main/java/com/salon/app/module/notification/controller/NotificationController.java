package com.salon.app.module.notification.controller;

import com.salon.app.module.auth.repository.UserRepository;
import com.salon.app.module.notification.dto.BroadcastRequest;
import com.salon.app.module.notification.service.NotificationService;
import com.salon.app.shared.enums.UserRole;
import com.salon.app.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> broadcast(@Valid @RequestBody BroadcastRequest request) {
        notificationService.sendBroadcast(request);
        return ResponseEntity.ok(ApiResponse.success("Broadcast queued", null));
    }

    @PostMapping("/campaign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> sendCampaign(@RequestParam String message) {
        List<String> phones = userRepository.findByRoleAndIsDeletedFalse(UserRole.CUSTOMER)
                .stream().map(u -> u.getPhoneNumber()).toList();
        notificationService.sendMarketingCampaign(message, phones);
        return ResponseEntity.ok(ApiResponse.success("Campaign queued for " + phones.size() + " customers", null));
    }
}
