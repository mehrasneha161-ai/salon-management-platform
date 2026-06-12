package com.salon.app.module.booking.service;

import com.salon.app.module.booking.repository.BookingRepository;
import com.salon.app.module.staff.repository.StaffProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlotAvailabilityService {

    private final BookingRepository bookingRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final SlotLockService slotLockService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final int SLOT_INTERVAL_MINUTES = 30;
    private static final int WORKING_START_HOUR = 9;
    private static final int WORKING_END_HOUR = 20;

    public List<String> getAvailableSlots(UUID outletId, UUID staffId, LocalDate date, int durationMinutes) {
        log.info("Getting available slots for outlet: {}, staff: {}, date: {}", outletId, staffId, date);
        List<String> allSlots = generateTimeSlots();
        List<String> bookedSlots = bookingRepository
                .findActiveBookingsForStaffOnDate(staffId, date)
                .stream()
                .map(b -> b.getScheduledTime().toString())
                .collect(Collectors.toList());
        return allSlots.stream()
                .filter(slot -> !bookedSlots.contains(slot))
                .filter(slot -> !slotLockService.isLocked(outletId, date,
                        LocalTime.parse(slot), staffId))
                .collect(Collectors.toList());
    }

    public void broadcastSlotUpdate(UUID outletId, LocalDate date) {
        String destination = "/topic/slots/" + outletId + "/" + date;
        messagingTemplate.convertAndSend(destination, "SLOT_UPDATED");
        log.info("Broadcasted slot update to: {}", destination);
    }

    private List<String> generateTimeSlots() {
        List<String> slots = new java.util.ArrayList<>();
        LocalTime current = LocalTime.of(WORKING_START_HOUR, 0);
        LocalTime end = LocalTime.of(WORKING_END_HOUR, 0);
        while (current.isBefore(end)) {
            slots.add(current.toString());
            current = current.plusMinutes(SLOT_INTERVAL_MINUTES);
        }
        return slots;
    }
}
