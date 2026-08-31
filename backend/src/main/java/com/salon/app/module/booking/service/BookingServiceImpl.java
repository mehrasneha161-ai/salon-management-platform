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
import com.salon.app.module.coupon.dto.request.CouponValidationRequest;
import com.salon.app.module.coupon.dto.response.CouponApplication;
import com.salon.app.module.coupon.entity.Coupon;
import com.salon.app.module.coupon.repository.CouponRepository;
import com.salon.app.module.coupon.service.CouponService;
import com.salon.app.module.outlet.entity.Outlet;
import com.salon.app.module.outlet.repository.OutletRepository;
import com.salon.app.module.service.entity.SalonService;
import com.salon.app.module.service.entity.ServicePackage;
import com.salon.app.module.service.repository.SalonServiceRepository;
import com.salon.app.module.service.repository.ServicePackageRepository;
import com.salon.app.module.staff.entity.StaffProfile;
import com.salon.app.module.staff.repository.StaffProfileRepository;
import com.salon.app.shared.enums.BookingStatus;
import com.salon.app.shared.enums.StaffStatus;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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
    private final CouponRepository couponRepository;
    private final CouponService couponService;
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
        User customer = findAvailableCustomer(customerId);
        Outlet outlet = findAvailableOutlet(request.getOutletId());
        StaffProfile staff = findAvailableStaff(request.getStaffId());
        validateStaffOutlet(staff, outlet);
        CatalogSelection selection = resolveCatalogSelection(request, outlet);

        BigDecimal subtotal = money(selection.subtotal());
        BigDecimal discount = BigDecimal.ZERO.setScale(2);
        BigDecimal total = subtotal;
        CouponApplication couponApplication = null;
        Coupon coupon = null;
        boolean hasCouponCode = request.getCouponCode() != null && !request.getCouponCode().isBlank();
        if (!hasCouponCode && hasExpectedCouponQuote(request)) {
            throw new BusinessException(
                    "Coupon quote details cannot be supplied without a coupon code");
        }

        if (hasCouponCode) {
            CouponValidationRequest couponRequest = new CouponValidationRequest();
            couponRequest.setCode(request.getCouponCode());
            couponRequest.setOutletId(outlet.getId());
            couponRequest.setServiceId(selection.service() == null ? null : selection.service().getId());
            couponRequest.setPackageId(selection.servicePackage() == null
                    ? null : selection.servicePackage().getId());

            // This lock is acquired before the external slot lock and remains held by the
            // surrounding booking transaction until it commits or rolls back.
            couponApplication = couponService.prepareApplication(customerId, couponRequest);
            validateExpectedCouponQuote(request, couponApplication);
            subtotal = couponApplication.subtotalAmount();
            discount = couponApplication.discountAmount();
            total = couponApplication.totalAmount();
            coupon = couponRepository.findById(couponApplication.couponId())
                    .filter(c -> !c.isDeleted())
                    .orElseThrow(() -> new BusinessException("Coupon is invalid"));
        }

        if (slotAvailabilityService.hasConflict(staff.getId(), request.getScheduledDate(),
                request.getScheduledTime(), selection.durationMinutes(), null)) {
            throw new SlotAlreadyLockedException(
                    "This time overlaps another booking for the selected stylist. Please pick a different slot.");
        }

        String bookingRef = generateBookingRef();
        String lockOwnerToken = bookingRef;
        boolean slotLockAcquired = false;
        try {
            boolean locked = slotLockService.tryLock(outlet.getId(), request.getScheduledDate(),
                    request.getScheduledTime(), staff.getId(), lockOwnerToken);
            if (!locked) {
                throw new SlotAlreadyLockedException(
                        "This slot is currently being booked by another customer. Please select a different slot.");
            }
            slotLockAcquired = true;
            registerSlotLockRollbackCleanup(outlet, staff, request, lockOwnerToken);

            Booking booking = Booking.builder()
                    .bookingRef(bookingRef)
                    .customer(customer)
                    .outlet(outlet)
                    .staff(staff)
                    .service(selection.service())
                    .servicePackage(selection.servicePackage())
                    .slotLockedAt(Instant.now())
                    .scheduledDate(request.getScheduledDate())
                    .scheduledTime(request.getScheduledTime())
                    .durationMinutes(selection.durationMinutes())
                    .status(BookingStatus.SLOT_LOCKED)
                    .subtotalAmount(subtotal)
                    .discountAmount(discount)
                    .totalAmount(total)
                    .coupon(coupon)
                    .couponCode(couponApplication == null ? null : couponApplication.couponCode())
                    .couponDiscountType(couponApplication == null ? null : couponApplication.discountType())
                    .couponDiscountValue(couponApplication == null ? null : couponApplication.discountValue())
                    .couponMaximumDiscount(couponApplication == null
                            ? null : couponApplication.maximumDiscount())
                    .notes(request.getNotes())
                    .build();

            bookingRepository.saveAndFlush(booking);
            if (couponApplication != null) {
                couponService.reserve(customerId, booking.getId(), couponApplication);
            }

            if (total.compareTo(BigDecimal.ZERO) == 0) {
                if (couponApplication != null) {
                    couponService.redeem(booking.getId());
                }
                booking.setStatus(BookingStatus.CONFIRMED);
                bookingRepository.save(booking);
                initializeNotificationAssociations(booking);
                eventPublisher.publishEvent(new BookingConfirmedEvent(this, booking));
            }

            slotAvailabilityService.broadcastSlotUpdate(outlet.getId(), request.getScheduledDate());
            log.info("Booking created with ref: {}", bookingRef);
            return toResponse(booking);
        } catch (RuntimeException | Error ex) {
            if (slotLockAcquired) {
                releaseSlotLockAfterFailure(outlet, staff, request, lockOwnerToken);
            }
            throw ex;
        }
    }

    @Override
    public PagedResponse<BookingResponse> getMyBookings(UUID customerId, Pageable pageable) {
        Page<Booking> page = bookingRepository
                .findByCustomerIdAndIsDeletedFalseOrderByCreatedAtDesc(customerId, pageable);
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Override
    public PagedResponse<BookingResponse> getBookings(
            UUID outletId, LocalDate date, BookingStatus status, Pageable pageable) {
        Page<Booking> page = bookingRepository.findByFilters(outletId, date, status, pageable);
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Override
    @Transactional
    public BookingResponse approveBooking(UUID bookingId, ApproveBookingRequest request) {
        log.info("Approving booking: {}", bookingId);
        Booking booking = findById(bookingId);
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            couponService.redeem(bookingId);
            return toResponse(booking);
        }
        if (booking.getStatus() != BookingStatus.PENDING
                && booking.getStatus() != BookingStatus.SLOT_LOCKED) {
            throw new BusinessException("Only a pending booking can be approved");
        }

        StaffProfile staff = findAvailableStaff(request.getStaffId());
        validateStaffOutlet(staff, booking.getOutlet());
        booking.setStaff(staff);
        couponService.redeem(bookingId);
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        initializeNotificationAssociations(booking);
        eventPublisher.publishEvent(new BookingConfirmedEvent(this, booking));
        return toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse rejectBooking(UUID bookingId, String reason) {
        log.info("Rejecting booking: {}", bookingId);
        Booking booking = findById(bookingId);
        if (booking.getStatus() == BookingStatus.REJECTED) {
            throw new BusinessException("Booking is already rejected");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.COMPLETED
                || booking.getStatus() == BookingStatus.IN_PROGRESS) {
            throw new BusinessException("This booking can no longer be rejected");
        }

        couponService.release(bookingId, "Booking rejected: " + normalizeReason(reason));
        booking.setStatus(BookingStatus.REJECTED);
        booking.setNotes(reason);
        if (booking.getStaff() != null) {
            slotLockService.releaseLock(booking.getOutlet().getId(), booking.getScheduledDate(),
                    booking.getScheduledTime(), booking.getStaff().getId(), booking.getBookingRef());
        }
        bookingRepository.save(booking);
        slotAvailabilityService.broadcastSlotUpdate(booking.getOutlet().getId(), booking.getScheduledDate());
        eventPublisher.publishEvent(new BookingCancelledEvent(this, booking));
        return toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse completeBooking(UUID bookingId) {
        log.info("Completing booking: {}", bookingId);
        Booking booking = findById(bookingId);
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            return toResponse(booking);
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED
                && booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new BusinessException(
                    "Only a confirmed or in-progress booking can be completed");
        }

        couponService.redeem(bookingId);
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
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("Booking is already cancelled");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED
                || booking.getStatus() == BookingStatus.REJECTED
                || booking.getStatus() == BookingStatus.IN_PROGRESS) {
            throw new BusinessException("This booking can no longer be cancelled");
        }

        couponService.release(bookingId, "Booking cancelled by customer");
        booking.setStatus(BookingStatus.CANCELLED);
        if (booking.getStaff() != null) {
            slotLockService.releaseLock(booking.getOutlet().getId(), booking.getScheduledDate(),
                    booking.getScheduledTime(), booking.getStaff().getId(), booking.getBookingRef());
        }
        slotAvailabilityService.broadcastSlotUpdate(booking.getOutlet().getId(), booking.getScheduledDate());
        return toResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingResponse rescheduleBooking(
            UUID bookingId, UUID customerId, RescheduleBookingRequest request) {
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
            targetStaff = findAvailableStaff(request.getStaffId());
        }
        if (targetStaff == null) {
            throw new BusinessException("A stylist is required to reschedule");
        }
        validateStaffOutlet(targetStaff, booking.getOutlet());

        UUID outletId = booking.getOutlet().getId();
        UUID currentStaffId = booking.getStaff() != null ? booking.getStaff().getId() : null;
        boolean sameSlot = targetStaff.getId().equals(currentStaffId)
                && request.getScheduledDate().equals(booking.getScheduledDate())
                && request.getScheduledTime().equals(booking.getScheduledTime());
        if (sameSlot) {
            throw new BusinessException("The booking is already at this date, time and stylist");
        }

        if (slotAvailabilityService.hasConflict(targetStaff.getId(), request.getScheduledDate(),
                request.getScheduledTime(), booking.getDurationMinutes(), booking.getId())) {
            throw new SlotAlreadyLockedException(
                    "That time overlaps another booking for the stylist. Please pick a different time.");
        }

        boolean locked = slotLockService.tryLock(outletId, request.getScheduledDate(),
                request.getScheduledTime(), targetStaff.getId(), booking.getBookingRef());
        if (!locked) {
            throw new SlotAlreadyLockedException(
                    "That slot is currently being booked by another customer. Please pick a different time.");
        }

        LocalDate oldDate = booking.getScheduledDate();
        if (booking.getStaff() != null) {
            slotLockService.releaseLock(outletId, booking.getScheduledDate(),
                    booking.getScheduledTime(), booking.getStaff().getId(), booking.getBookingRef());
        }

        // Pricing and coupon snapshot fields intentionally remain unchanged on reschedule.
        booking.setStaff(targetStaff);
        booking.setScheduledDate(request.getScheduledDate());
        booking.setScheduledTime(request.getScheduledTime());
        booking.setStatus(BookingStatus.SLOT_LOCKED);
        bookingRepository.save(booking);

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

    private User findAvailableCustomer(UUID id) {
        return userRepository.findById(id)
                .filter(user -> !user.isDeleted() && user.isActive())
                .orElseThrow(() -> new BusinessException("Customer account is unavailable"));
    }

    private Outlet findAvailableOutlet(UUID id) {
        return outletRepository.findById(id)
                .filter(outlet -> !outlet.isDeleted() && outlet.isActive())
                .orElseThrow(() -> new BusinessException("Outlet is unavailable"));
    }

    private StaffProfile findAvailableStaff(UUID id) {
        return staffProfileRepository.findById(id)
                .filter(staff -> !staff.isDeleted())
                .filter(staff -> !staff.getUser().isDeleted() && staff.getUser().isActive())
                .filter(staff -> staff.getStatus() != StaffStatus.OFF_DUTY)
                .orElseThrow(() -> new BusinessException("Staff is unavailable"));
    }

    private void validateStaffOutlet(StaffProfile staff, Outlet outlet) {
        if (!staff.getOutlet().getId().equals(outlet.getId())) {
            throw new BusinessException("Staff does not belong to the selected outlet");
        }
    }

    private CatalogSelection resolveCatalogSelection(CreateBookingRequest request, Outlet outlet) {
        if ((request.getServiceId() == null) == (request.getPackageId() == null)) {
            throw new BusinessException("Select exactly one service or package");
        }

        if (request.getServiceId() != null) {
            SalonService service = serviceRepository.findById(request.getServiceId())
                    .filter(item -> !item.isDeleted() && item.isActive())
                    .orElseThrow(() -> new BusinessException("Service is unavailable"));
            validateCatalogOutlet(service.getOutlet(), outlet, "Service");
            if (service.getDurationMinutes() <= 0 || service.getPrice() == null
                    || service.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("Service pricing or duration is invalid");
            }
            return new CatalogSelection(service, null, service.getPrice(), service.getDurationMinutes());
        }

        ServicePackage servicePackage = packageRepository.findById(request.getPackageId())
                .filter(item -> !item.isDeleted() && item.isActive())
                .orElseThrow(() -> new BusinessException("Package is unavailable"));
        validateCatalogOutlet(servicePackage.getOutlet(), outlet, "Package");
        int duration = servicePackage.getServices().stream()
                .mapToInt(SalonService::getDurationMinutes)
                .sum();
        if (duration <= 0 || servicePackage.getPrice() == null
                || servicePackage.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Package pricing or duration is invalid");
        }
        // Package.price is authoritative; discountPct is already represented by that price.
        return new CatalogSelection(null, servicePackage, servicePackage.getPrice(), duration);
    }

    private void validateCatalogOutlet(Outlet catalogOutlet, Outlet selectedOutlet, String itemType) {
        if (catalogOutlet != null && !catalogOutlet.getId().equals(selectedOutlet.getId())) {
            throw new BusinessException(itemType + " is not available at the selected outlet");
        }
    }

    private Booking findById(UUID id) {
        return bookingRepository.findByIdForUpdate(id)
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

    private void initializeNotificationAssociations(Booking booking) {
        booking.getCustomer().getFullName();
        booking.getCustomer().getEmail();
        booking.getCustomer().getPhoneNumber();
        booking.getOutlet().getName();
    }

    private void registerSlotLockRollbackCleanup(
            Outlet outlet, StaffProfile staff, CreateBookingRequest request, String sessionId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    releaseSlotLockAfterFailure(outlet, staff, request, sessionId);
                }
            }
        });
    }

    private void releaseSlotLockAfterFailure(
            Outlet outlet, StaffProfile staff, CreateBookingRequest request, String sessionId) {
        try {
            slotLockService.releaseLock(outlet.getId(), request.getScheduledDate(),
                    request.getScheduledTime(), staff.getId(), sessionId);
        } catch (RuntimeException releaseFailure) {
            log.error("Failed to release slot lock after booking creation failed", releaseFailure);
        }
    }

    private boolean hasExpectedCouponQuote(CreateBookingRequest request) {
        return request.getExpectedCouponId() != null
                || request.getExpectedSubtotalAmount() != null
                || request.getExpectedDiscountAmount() != null
                || request.getExpectedTotalAmount() != null;
    }

    private void validateExpectedCouponQuote(
            CreateBookingRequest request, CouponApplication application) {
        if (request.getExpectedCouponId() == null
                || request.getExpectedSubtotalAmount() == null
                || request.getExpectedDiscountAmount() == null
                || request.getExpectedTotalAmount() == null
                || !request.getExpectedCouponId().equals(application.couponId())
                || request.getExpectedSubtotalAmount().compareTo(application.subtotalAmount()) != 0
                || request.getExpectedDiscountAmount().compareTo(application.discountAmount()) != 0
                || request.getExpectedTotalAmount().compareTo(application.totalAmount()) != 0) {
            throw new BusinessException(
                    "Coupon quote changed. Please apply the coupon again.");
        }
    }

    private String normalizeReason(String reason) {
        return reason == null || reason.isBlank() ? "No reason provided" : reason.trim();
    }

    private BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
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
                .subtotalAmount(b.getSubtotalAmount())
                .discountAmount(b.getDiscountAmount())
                .totalAmount(b.getTotalAmount())
                .couponCode(b.getCouponCode())
                .notes(b.getNotes())
                .createdAt(b.getCreatedAt())
                .build();
    }

    private record CatalogSelection(
            SalonService service,
            ServicePackage servicePackage,
            BigDecimal subtotal,
            int durationMinutes) {
    }
}
