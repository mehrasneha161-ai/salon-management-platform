package com.salon.app.module.booking.service;

import com.salon.app.module.auth.entity.User;
import com.salon.app.module.auth.repository.UserRepository;
import com.salon.app.module.booking.dto.request.ApproveBookingRequest;
import com.salon.app.module.booking.dto.request.CreateBookingRequest;
import com.salon.app.module.booking.dto.request.RescheduleBookingRequest;
import com.salon.app.module.booking.dto.response.BookingResponse;
import com.salon.app.module.booking.entity.Booking;
import com.salon.app.module.booking.event.BookingCancelledEvent;
import com.salon.app.module.booking.event.BookingConfirmedEvent;
import com.salon.app.module.booking.repository.BookingRepository;
import com.salon.app.module.outlet.entity.Outlet;
import com.salon.app.module.outlet.repository.OutletRepository;
import com.salon.app.module.service.entity.SalonService;
import com.salon.app.module.service.entity.ServicePackage;
import com.salon.app.module.service.repository.SalonServiceRepository;
import com.salon.app.module.service.repository.ServicePackageRepository;
import com.salon.app.module.staff.entity.StaffProfile;
import com.salon.app.module.staff.repository.StaffProfileRepository;
import com.salon.app.shared.enums.BookingStatus;
import com.salon.app.shared.exception.BusinessException;
import com.salon.app.shared.exception.ResourceNotFoundException;
import com.salon.app.shared.exception.SlotAlreadyLockedException;
import com.salon.app.shared.response.PagedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final OutletRepository outletRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final SalonServiceRepository serviceRepository;
    private final ServicePackageRepository packageRepository;
    private final SlotLockService slotLockService;
    private final SlotAvailabilityService slotAvailabilityService;
    private final ApplicationEventPublisher eventPublisher;

    // Booking reference: BK-<yyyyMMdd>-<random 6-char> — restart & multi-instance safe.
    private static final String REF_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int REF_SUFFIX_LENGTH = 6;
    private static final int REF_MAX_ATTEMPTS = 5;

    @Override
    @Transactional
    public BookingResponse createBooking(UUID customerId, CreateBookingRequest request) {
        log.info("Creating booking for customer: {}", customerId);
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", customerId));
        Outlet outlet = outletRepository.findById(request.getOutletId())
                .orElseThrow(() -> new ResourceNotFoundException("Outlet", "id", request.getOutletId()));
        StaffProfile staff = staffProfileRepository.findById(request.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "id", request.getStaffId()));

        // Resolve the service/package first so we know how long the booking is.
        SalonService service = null;
        ServicePackage pkg = null;
        BigDecimal amount = BigDecimal.ZERO;
        int duration = 30;

        if (request.getServiceId() != null) {
            service = serviceRepository.findById(request.getServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service", "id", request.getServiceId()));
            amount = service.getPrice();
            duration = service.getDurationMinutes();
        } else if (request.getPackageId() != null) {
            pkg = packageRepository.findById(request.getPackageId())
                    .orElseThrow(() -> new ResourceNotFoundException("Package", "id", request.getPackageId()));
            amount = pkg.getPrice();
            duration = pkg.getServices().stream().mapToInt(SalonService::getDurationMinutes).sum();
        }

        // Reject if the WHOLE service window would overlap an existing booking
        // for this stylist (duration-aware, e.g. a 60-min service at 09:00 blocks 09:30).
        if (slotAvailabilityService.hasConflict(request.getStaffId(), request.getScheduledDate(),
                request.getScheduledTime(), duration, null)) {
            throw new SlotAlreadyLockedException("This time overlaps another booking for the selected stylist. Please pick a different slot.");
        }

        // Attempt slot lock
        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();
        boolean locked = slotLockService.tryLock(
                request.getOutletId(), request.getScheduledDate(),
                request.getScheduledTime(), request.getStaffId(), sessionId);
        if (!locked) {
            throw new SlotAlreadyLockedException("This slot is currently being booked by another customer. Please select a different slot.");
        }

        String bookingRef = generateBookingRef();
        Booking booking = Booking.builder()
                .bookingRef(bookingRef)
                .customer(customer)
                .outlet(outlet)
                .staff(staff)
                .service(service)
                .servicePackage(pkg)
                .scheduledDate(request.getScheduledDate())
                .scheduledTime(request.getScheduledTime())
                .durationMinutes(duration)
                .status(BookingStatus.SLOT_LOCKED)
                .totalAmount(amount)
                .notes(request.getNotes())
                .build();

        bookingRepository.save(booking);
        slotAvailabilityService.broadcastSlotUpdate(request.getOutletId(), request.getScheduledDate());
        log.info("Booking created with ref: {}", bookingRef);
        return toResponse(booking);
    }

    @Override
    public PagedResponse<BookingResponse> getMyBookings(UUID customerId, Pageable pageable) {
        Page<Booking> page = bookingRepository
                .findByCustomerIdAndIsDeletedFalseOrderByCreatedAtDesc(customerId, pageable);
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Override
    public PagedResponse<BookingResponse> getBookings(UUID outletId, LocalDate date, BookingStatus status, Pageable pageable) {
        Page<Booking> page = bookingRepository.findByFilters(outletId, date, status, pageable);
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Override
    @Transactional
    public BookingResponse approveBooking(UUID bookingId, ApproveBookingRequest request) {
        log.info("Approving booking: {}", bookingId);
        Booking booking = findById(bookingId);
        StaffProfile staff = staffProfileRepository.findById(request.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "id", request.getStaffId()));
        booking.setStaff(staff);
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        // Initialise lazy associations in-transaction so the async notification
        // handlers can read them safely after the entity is detached.
        booking.getCustomer().getFullName();
        booking.getCustomer().getEmail();
        booking.getCustomer().getPhoneNumber();
        booking.getOutlet().getName();
        eventPublisher.publishEvent(new BookingConfirmedEvent(this, booking));
        return toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse rejectBooking(UUID bookingId, String reason) {
        log.info("Rejecting booking: {}", bookingId);
        Booking booking = findById(bookingId);
        booking.setStatus(BookingStatus.REJECTED);
        booking.setNotes(reason);
        slotLockService.releaseLock(booking.getOutlet().getId(),
                booking.getScheduledDate(), booking.getScheduledTime(),
                booking.getStaff() != null ? booking.getStaff().getId() : UUID.randomUUID());
        bookingRepository.save(booking);
        eventPublisher.publishEvent(new BookingCancelledEvent(this, booking));
        return toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse completeBooking(UUID bookingId) {
        log.info("Completing booking: {}", bookingId);
        Booking booking = findById(bookingId);
        booking.setStatus(BookingStatus.COMPLETED);
        return toResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(UUID bookingId, UUID customerId) {
        log.info("Cancelling booking: {} by customer: {}", bookingId, customerId);
        Booking booking = findById(bookingId);
        if (!booking.getCustomer().getId().equals(customerId)) {
            throw new BusinessException("You can only cancel your own bookings");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessException("Cannot cancel a completed booking");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        if (booking.getStaff() != null) {
            slotLockService.releaseLock(booking.getOutlet().getId(),
                    booking.getScheduledDate(), booking.getScheduledTime(), booking.getStaff().getId());
        }
        slotAvailabilityService.broadcastSlotUpdate(booking.getOutlet().getId(), booking.getScheduledDate());
        return toResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingResponse rescheduleBooking(UUID bookingId, UUID customerId, RescheduleBookingRequest request) {
        log.info("Rescheduling booking: {} by customer: {}", bookingId, customerId);
        Booking booking = findById(bookingId);
        if (!booking.getCustomer().getId().equals(customerId)) {
            throw new BusinessException("You can only reschedule your own bookings");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED
                || booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.REJECTED) {
            throw new BusinessException("This booking can no longer be rescheduled");
        }

        // Target stylist: keep the current one unless a new one is requested.
        StaffProfile targetStaff = booking.getStaff();
        if (request.getStaffId() != null) {
            targetStaff = staffProfileRepository.findById(request.getStaffId())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff", "id", request.getStaffId()));
        }
        if (targetStaff == null) {
            throw new BusinessException("A stylist is required to reschedule");
        }

        UUID outletId = booking.getOutlet().getId();
        UUID currentStaffId = booking.getStaff() != null ? booking.getStaff().getId() : null;
        boolean sameSlot = targetStaff.getId().equals(currentStaffId)
                && request.getScheduledDate().equals(booking.getScheduledDate())
                && request.getScheduledTime().equals(booking.getScheduledTime());
        if (sameSlot) {
            throw new BusinessException("The booking is already at this date, time and stylist");
        }

        // Duration-aware overlap check for the new slot (ignoring this booking itself).
        if (slotAvailabilityService.hasConflict(targetStaff.getId(), request.getScheduledDate(),
                request.getScheduledTime(), booking.getDurationMinutes(), booking.getId())) {
            throw new SlotAlreadyLockedException("That time overlaps another booking for the stylist. Please pick a different time.");
        }

        // Lock the NEW slot first; only then give up the old one.
        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();
        boolean locked = slotLockService.tryLock(outletId, request.getScheduledDate(),
                request.getScheduledTime(), targetStaff.getId(), sessionId);
        if (!locked) {
            throw new SlotAlreadyLockedException("That slot is currently being booked by another customer. Please pick a different time.");
        }

        LocalDate oldDate = booking.getScheduledDate();
        if (booking.getStaff() != null) {
            slotLockService.releaseLock(outletId, booking.getScheduledDate(),
                    booking.getScheduledTime(), booking.getStaff().getId());
        }

        // Apply the change and re-enter the pipeline for admin confirmation.
        booking.setStaff(targetStaff);
        booking.setScheduledDate(request.getScheduledDate());
        booking.setScheduledTime(request.getScheduledTime());
        booking.setStatus(BookingStatus.SLOT_LOCKED);
        bookingRepository.save(booking);

        // Refresh both the old and the new date's slot boards.
        slotAvailabilityService.broadcastSlotUpdate(outletId, oldDate);
        slotAvailabilityService.broadcastSlotUpdate(outletId, request.getScheduledDate());
        log.info("Booking {} rescheduled to {} {}", booking.getBookingRef(),
                request.getScheduledDate(), request.getScheduledTime());
        return toResponse(booking);
    }

    @Override
    public List<BookingResponse> getAssignedBookings(UUID staffUserId, LocalDate date) {
        StaffProfile staff = staffProfileRepository.findByUserIdAndIsDeletedFalse(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", "userId", staffUserId));
        return bookingRepository.findAssignedBookings(staff.getId(), date)
                .stream().map(this::toResponse).toList();
    }

    private Booking findById(UUID id) {
        return bookingRepository.findById(id)
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));
    }

    private String generateBookingRef() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int attempt = 0; attempt < REF_MAX_ATTEMPTS; attempt++) {
            String ref = "BK-" + datePart + "-" + randomRefSuffix();
            if (!bookingRepository.existsByBookingRef(ref)) {
                return ref;
            }
            log.warn("Booking ref collision on attempt {}: {}", attempt + 1, ref);
        }
        throw new BusinessException("Could not generate a unique booking reference. Please retry.");
    }

    private String randomRefSuffix() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(REF_SUFFIX_LENGTH);
        for (int i = 0; i < REF_SUFFIX_LENGTH; i++) {
            sb.append(REF_ALPHABET.charAt(random.nextInt(REF_ALPHABET.length())));
        }
        return sb.toString();
    }

    private BookingResponse toResponse(Booking b) {
        return BookingResponse.builder()
                .id(b.getId())
                .bookingRef(b.getBookingRef())
                .customerName(b.getCustomer().getFullName())
                .customerPhone(b.getCustomer().getPhoneNumber())
                .outletId(b.getOutlet().getId())
                .outletName(b.getOutlet().getName())
                .staffId(b.getStaff() != null ? b.getStaff().getId() : null)
                .staffName(b.getStaff() != null ? b.getStaff().getUser().getFullName() : null)
                .serviceName(b.getService() != null ? b.getService().getName() : null)
                .packageName(b.getServicePackage() != null ? b.getServicePackage().getName() : null)
                .scheduledDate(b.getScheduledDate())
                .scheduledTime(b.getScheduledTime())
                .durationMinutes(b.getDurationMinutes())
                .status(b.getStatus().name())
                .totalAmount(b.getTotalAmount())
                .notes(b.getNotes())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
