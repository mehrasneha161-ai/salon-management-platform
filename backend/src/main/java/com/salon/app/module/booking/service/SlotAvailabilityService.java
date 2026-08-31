package com.salon.app.module.booking.service;

import com.salon.app.module.booking.repository.BookingRepository;
import com.salon.app.module.outlet.entity.Outlet;
import com.salon.app.module.outlet.repository.OutletRepository;
import com.salon.app.module.staff.entity.StaffProfile;
import com.salon.app.module.staff.repository.StaffLeaveRepository;
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
    private final StaffLeaveRepository staffLeaveRepository;
    private final OutletRepository outletRepository;
    private final SlotLockService slotLockService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final int SLOT_INTERVAL_MINUTES = 30;
    private static final LocalTime DEFAULT_OPEN = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_CLOSE = LocalTime.of(20, 0);

    public List<String> getAvailableSlots(UUID outletId, UUID staffId, LocalDate date, int durationMinutes) {
        log.info("Getting available slots for outlet: {}, staff: {}, date: {}", outletId, staffId, date);

        // 1. If the staff is on leave that day, nothing is available.
        if (staffLeaveRepository
                .existsByStaffIdAndIsDeletedFalseAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        staffId, date, date)) {
            log.info("Staff {} is on leave on {}", staffId, date);
            return List.of();
        }

        // 2. Effective working window = outlet hours narrowed by the staff shift.
        LocalTime open = DEFAULT_OPEN;
        LocalTime close = DEFAULT_CLOSE;

        Outlet outlet = outletRepository.findById(outletId).orElse(null);
        if (outlet != null) {
            if (outlet.getOpeningTime() != null) open = outlet.getOpeningTime();
            if (outlet.getClosingTime() != null) close = outlet.getClosingTime();
        }
        StaffProfile staff = staffProfileRepository.findById(staffId).orElse(null);
        if (staff != null) {
            if (staff.getShiftStart() != null && staff.getShiftStart().isAfter(open)) open = staff.getShiftStart();
            if (staff.getShiftEnd() != null && staff.getShiftEnd().isBefore(close)) close = staff.getShiftEnd();
        }
        if (!open.isBefore(close)) {
            return List.of(); // empty or invalid window (e.g. shift outside outlet hours)
        }

        // 3. Generate the grid and remove booked + locked slots.
        List<String> allSlots = generateTimeSlots(open, close);
        List<String> bookedSlots = bookingRepository
                .findActiveBookingsForStaffOnDate(staffId, date)
                .stream()
                .map(b -> b.getScheduledTime().toString())
                .collect(Collectors.toList());
        return allSlots.stream()
                .filter(slot -> !bookedSlots.contains(slot))
                .filter(slot -> !slotLockService.isLocked(outletId, date, LocalTime.parse(slot), staffId))
                .collect(Collectors.toList());
    }

    public void broadcastSlotUpdate(UUID outletId, LocalDate date) {
        String destination = "/topic/slots/" + outletId + "/" + date;
        messagingTemplate.convertAndSend(destination, "SLOT_UPDATED");
        log.info("Broadcasted slot update to: {}", destination);
    }

    private List<String> generateTimeSlots(LocalTime start, LocalTime end) {
        List<String> slots = new java.util.ArrayList<>();
        LocalTime current = start;
        while (current.isBefore(end)) {
            slots.add(current.toString());
            current = current.plusMinutes(SLOT_INTERVAL_MINUTES);
        }
        return slots;
    }
}
