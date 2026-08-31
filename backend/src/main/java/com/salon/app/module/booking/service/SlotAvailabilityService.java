package com.salon.app.module.booking.service;

import com.salon.app.module.booking.entity.Booking;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        log.info("Getting available slots for outlet: {}, staff: {}, date: {}, duration: {}",
                outletId, staffId, date, durationMinutes);

        // 1. Staff on leave that day -> nothing is available.
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
            return List.of();
        }

        // 3. Build busy intervals from existing bookings (start .. start+duration).
        int duration = durationMinutes <= 0 ? SLOT_INTERVAL_MINUTES : durationMinutes;
        List<int[]> busy = new ArrayList<>();
        for (Booking b : bookingRepository.findActiveBookingsForStaffOnDate(staffId, date)) {
            int bs = toMinutes(b.getScheduledTime());
            busy.add(new int[]{bs, bs + b.getDurationMinutes()});
        }

        // 4. A start slot is available only if the WHOLE service [start, start+duration)
        //    fits before closing, overlaps no existing booking, and isn't locked.
        int openM = toMinutes(open);
        int closeM = toMinutes(close);
        List<String> result = new ArrayList<>();
        for (int startM = openM; startM + duration <= closeM; startM += SLOT_INTERVAL_MINUTES) {
            int endM = startM + duration;
            boolean overlaps = false;
            for (int[] iv : busy) {
                if (startM < iv[1] && iv[0] < endM) { overlaps = true; break; }
            }
            if (overlaps) continue;
            LocalTime slot = LocalTime.of(startM / 60, startM % 60);
            if (slotLockService.isLocked(outletId, date, slot, staffId)) continue;
            result.add(slot.toString());
        }
        return result;
    }

    /**
     * True if a service of {@code durationMinutes} starting at {@code start} would
     * overlap any existing active booking for the staff on that date. Pass
     * {@code excludeBookingId} to ignore a booking being rescheduled.
     */
    public boolean hasConflict(UUID staffId, LocalDate date, LocalTime start,
                               int durationMinutes, UUID excludeBookingId) {
        int s = toMinutes(start);
        int e = s + (durationMinutes <= 0 ? SLOT_INTERVAL_MINUTES : durationMinutes);
        for (Booking b : bookingRepository.findActiveBookingsForStaffOnDate(staffId, date)) {
            if (excludeBookingId != null && excludeBookingId.equals(b.getId())) continue;
            int bs = toMinutes(b.getScheduledTime());
            int be = bs + b.getDurationMinutes();
            if (s < be && bs < e) return true;
        }
        return false;
    }

    public void broadcastSlotUpdate(UUID outletId, LocalDate date) {
        String destination = "/topic/slots/" + outletId + "/" + date;
        messagingTemplate.convertAndSend(destination, "SLOT_UPDATED");
        log.info("Broadcasted slot update to: {}", destination);
    }

    private int toMinutes(LocalTime t) {
        return t.getHour() * 60 + t.getMinute();
    }
}
