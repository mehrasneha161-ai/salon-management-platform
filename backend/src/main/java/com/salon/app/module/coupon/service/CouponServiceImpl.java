package com.salon.app.module.coupon.service;

import com.salon.app.module.auth.entity.User;
import com.salon.app.module.auth.repository.UserRepository;
import com.salon.app.module.booking.entity.Booking;
import com.salon.app.module.booking.repository.BookingRepository;
import com.salon.app.module.coupon.dto.request.CouponRequest;
import com.salon.app.module.coupon.dto.request.CouponValidationRequest;
import com.salon.app.module.coupon.dto.response.CouponApplication;
import com.salon.app.module.coupon.dto.response.CouponResponse;
import com.salon.app.module.coupon.dto.response.CouponValidationResponse;
import com.salon.app.module.coupon.entity.Coupon;
import com.salon.app.module.coupon.entity.CouponRedemption;
import com.salon.app.module.coupon.repository.CouponRedemptionRepository;
import com.salon.app.module.coupon.repository.CouponRepository;
import com.salon.app.module.outlet.entity.Outlet;
import com.salon.app.module.outlet.repository.OutletRepository;
import com.salon.app.module.service.entity.SalonService;
import com.salon.app.module.service.entity.ServicePackage;
import com.salon.app.module.service.repository.SalonServiceRepository;
import com.salon.app.module.service.repository.ServicePackageRepository;
import com.salon.app.shared.enums.CouponDiscountType;
import com.salon.app.shared.enums.CouponRedemptionStatus;
import com.salon.app.shared.enums.UserRole;
import com.salon.app.shared.exception.BusinessException;
import com.salon.app.shared.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final EnumSet<CouponRedemptionStatus> LIMIT_COUNTING_STATUSES =
            EnumSet.of(CouponRedemptionStatus.RESERVED, CouponRedemptionStatus.REDEEMED);

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;
    private final UserRepository userRepository;
    private final OutletRepository outletRepository;
    private final SalonServiceRepository salonServiceRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final BookingRepository bookingRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> getAllCoupons() {
        return couponRepository.findByIsDeletedFalseOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse getCouponById(UUID id) {
        return toResponse(findCoupon(id));
    }

    @Override
    @Transactional
    public CouponResponse createCoupon(CouponRequest request) {
        validateCouponRequest(request);
        String normalizedCode = normalizeCode(request.getCode());
        if (couponRepository.existsByNormalizedCode(normalizedCode)) {
            throw new BusinessException("Coupon code already exists");
        }

        CouponScope scope = resolveAdminScope(request);
        Coupon coupon = Coupon.builder()
                .code(request.getCode().trim())
                .normalizedCode(normalizedCode)
                .name(request.getName().trim())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(money(request.getDiscountValue()))
                .minimumSpend(money(defaultZero(request.getMinimumSpend())))
                .maximumDiscount(nullableMoney(request.getMaximumDiscount()))
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .usageLimit(request.getUsageLimit())
                .perCustomerLimit(request.getPerCustomerLimit())
                .outlet(scope.outlet())
                .service(scope.service())
                .servicePackage(scope.servicePackage())
                .isActive(request.isActive())
                .build();
        log.info("Creating coupon: {}", normalizedCode);
        return toResponse(saveCoupon(coupon));
    }

    @Override
    @Transactional
    public CouponResponse updateCoupon(UUID id, CouponRequest request) {
        validateCouponRequest(request);
        Coupon coupon = findCoupon(id);
        String normalizedCode = normalizeCode(request.getCode());
        if (couponRepository.existsByNormalizedCodeAndIdNot(normalizedCode, id)) {
            throw new BusinessException("Coupon code already exists");
        }

        CouponScope scope = resolveAdminScope(request);
        coupon.setCode(request.getCode().trim());
        coupon.setNormalizedCode(normalizedCode);
        coupon.setName(request.getName().trim());
        coupon.setDescription(request.getDescription());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(money(request.getDiscountValue()));
        coupon.setMinimumSpend(money(defaultZero(request.getMinimumSpend())));
        coupon.setMaximumDiscount(nullableMoney(request.getMaximumDiscount()));
        coupon.setValidFrom(request.getValidFrom());
        coupon.setValidUntil(request.getValidUntil());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setPerCustomerLimit(request.getPerCustomerLimit());
        coupon.setOutlet(scope.outlet());
        coupon.setService(scope.service());
        coupon.setServicePackage(scope.servicePackage());
        coupon.setActive(request.isActive());
        log.info("Updating coupon: {}", id);
        return toResponse(saveCoupon(coupon));
    }

    @Override
    @Transactional
    public CouponResponse toggleCoupon(UUID id) {
        Coupon coupon = findCoupon(id);
        coupon.setActive(!coupon.isActive());
        log.info("Toggled coupon {} to active={}", id, coupon.isActive());
        return toResponse(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    public void deleteCoupon(UUID id) {
        Coupon coupon = findCoupon(id);
        coupon.setActive(false);
        coupon.setDeleted(true);
        couponRepository.save(coupon);
        log.info("Soft-deleted coupon: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(UUID customerId, CouponValidationRequest request) {
        User customer = findCustomer(customerId);
        CatalogContext context = resolveCatalogContext(request);
        Coupon coupon = couponRepository.findByNormalizedCodeAndIsDeletedFalse(normalizeCode(request.getCode()))
                .orElseThrow(() -> new BusinessException("Coupon is invalid"));
        CouponApplication application = calculateApplication(coupon, customer, context, Instant.now());
        return toValidationResponse(coupon, application);
    }

    @Override
    @Transactional
    public CouponApplication prepareApplication(UUID customerId, CouponValidationRequest request) {
        User customer = findCustomer(customerId);
        CatalogContext context = resolveCatalogContext(request);
        Coupon coupon = couponRepository.findByNormalizedCodeForUpdate(normalizeCode(request.getCode()))
                .orElseThrow(() -> new BusinessException("Coupon is invalid"));
        return calculateApplication(coupon, customer, context, Instant.now());
    }

    @Override
    @Transactional
    public void reserve(UUID customerId, UUID bookingId, CouponApplication application) {
        if (application == null || application.couponId() == null) {
            throw new BusinessException("Prepared coupon application is required");
        }

        Coupon coupon = couponRepository.findByIdForUpdate(application.couponId())
                .orElseThrow(() -> new BusinessException("Coupon is invalid"));
        User customer = findCustomer(customerId);
        Booking booking = findBooking(bookingId);
        entityManager.lock(booking, LockModeType.PESSIMISTIC_WRITE);

        CouponRedemption existing = couponRedemptionRepository.findByBookingIdAndIsDeletedFalse(bookingId)
                .orElse(null);
        if (existing != null) {
            ensureSameReservation(existing, coupon, customer);
            if (existing.getStatus() == CouponRedemptionStatus.RELEASED) {
                throw new BusinessException("Released coupon reservation cannot be reserved again");
            }
            return;
        }

        ensureBookingOwnershipAndContext(booking, customer, application);
        CatalogContext context = resolveCatalogContext(
                application.outletId(), application.serviceId(), application.packageId());
        CouponApplication current = calculateApplication(coupon, customer, context, Instant.now());
        ensureApplicationUnchanged(application, current);

        CouponRedemption redemption = CouponRedemption.builder()
                .coupon(coupon)
                .customer(customer)
                .booking(booking)
                .status(CouponRedemptionStatus.RESERVED)
                .discountAmount(current.discountAmount())
                .reservedAt(Instant.now())
                .build();
        couponRedemptionRepository.save(redemption);
        log.info("Reserved coupon {} for booking {}", coupon.getNormalizedCode(), bookingId);
    }

    @Override
    @Transactional
    public void redeem(UUID bookingId) {
        CouponRedemption redemption = couponRedemptionRepository.findByBookingIdForUpdate(bookingId)
                .orElse(null);
        if (redemption == null || redemption.getStatus() == CouponRedemptionStatus.REDEEMED) {
            return;
        }
        if (redemption.getStatus() == CouponRedemptionStatus.RELEASED) {
            throw new BusinessException("Released coupon reservation cannot be redeemed");
        }

        redemption.setStatus(CouponRedemptionStatus.REDEEMED);
        redemption.setRedeemedAt(Instant.now());
        couponRedemptionRepository.save(redemption);
        log.info("Redeemed coupon reservation for booking {}", bookingId);
    }

    @Override
    @Transactional
    public void release(UUID bookingId, String reason) {
        CouponRedemption redemption = couponRedemptionRepository.findByBookingIdForUpdate(bookingId)
                .orElse(null);
        if (redemption == null
                || redemption.getStatus() == CouponRedemptionStatus.RELEASED
                || redemption.getStatus() == CouponRedemptionStatus.REDEEMED) {
            return;
        }

        redemption.setStatus(CouponRedemptionStatus.RELEASED);
        redemption.setReleasedAt(Instant.now());
        redemption.setReleaseReason(normalizeReason(reason));
        couponRedemptionRepository.save(redemption);
        log.info("Released coupon reservation for booking {}", bookingId);
    }

    private CouponApplication calculateApplication(
            Coupon coupon, User customer, CatalogContext context, Instant now) {
        validateAvailability(coupon, customer.getId(), context, now);
        BigDecimal subtotal = money(context.subtotal());
        BigDecimal discount = calculateDiscount(coupon, subtotal);
        return new CouponApplication(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDiscountType(),
                money(coupon.getDiscountValue()),
                nullableMoney(coupon.getMaximumDiscount()),
                subtotal,
                discount,
                money(subtotal.subtract(discount)),
                context.outlet().getId(),
                context.service() == null ? null : context.service().getId(),
                context.servicePackage() == null ? null : context.servicePackage().getId(),
                now);
    }

    private void validateAvailability(Coupon coupon, UUID customerId, CatalogContext context, Instant now) {
        if (!coupon.isActive()) {
            throw new BusinessException("Coupon is inactive");
        }
        if (now.isBefore(coupon.getValidFrom()) || !now.isBefore(coupon.getValidUntil())) {
            throw new BusinessException("Coupon is outside its validity period");
        }
        if (context.subtotal().compareTo(coupon.getMinimumSpend()) < 0) {
            throw new BusinessException("Minimum spend requirement is not met");
        }
        validateScope(coupon, context);
        validateUsageLimits(coupon, customerId);
    }

    private void validateUsageLimits(Coupon coupon, UUID customerId) {
        if (coupon.getUsageLimit() != null) {
            long globalUsage = couponRedemptionRepository
                    .countByCouponIdAndStatusInAndIsDeletedFalse(coupon.getId(), LIMIT_COUNTING_STATUSES);
            if (globalUsage >= coupon.getUsageLimit()) {
                throw new BusinessException("Coupon usage limit has been reached");
            }
        }
        if (coupon.getPerCustomerLimit() != null) {
            long customerUsage = couponRedemptionRepository
                    .countByCouponIdAndCustomerIdAndStatusInAndIsDeletedFalse(
                            coupon.getId(), customerId, LIMIT_COUNTING_STATUSES);
            if (customerUsage >= coupon.getPerCustomerLimit()) {
                throw new BusinessException("Customer coupon usage limit has been reached");
            }
        }
    }

    private void validateScope(Coupon coupon, CatalogContext context) {
        if (coupon.getOutlet() != null && !coupon.getOutlet().getId().equals(context.outlet().getId())) {
            throw new BusinessException("Coupon is not valid for this outlet");
        }
        if (coupon.getService() != null && (context.service() == null
                || !coupon.getService().getId().equals(context.service().getId()))) {
            throw new BusinessException("Coupon is not valid for this service");
        }
        if (coupon.getServicePackage() != null && (context.servicePackage() == null
                || !coupon.getServicePackage().getId().equals(context.servicePackage().getId()))) {
            throw new BusinessException("Coupon is not valid for this package");
        }
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal) {
        BigDecimal discount;
        if (coupon.getDiscountType() == CouponDiscountType.PERCENTAGE) {
            discount = subtotal.multiply(coupon.getDiscountValue())
                    .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
        } else {
            discount = money(coupon.getDiscountValue());
        }
        if (coupon.getMaximumDiscount() != null && discount.compareTo(coupon.getMaximumDiscount()) > 0) {
            discount = coupon.getMaximumDiscount();
        }
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }
        return money(discount);
    }

    private CatalogContext resolveCatalogContext(CouponValidationRequest request) {
        if (request == null) {
            throw new BusinessException("Coupon validation request is required");
        }
        return resolveCatalogContext(request.getOutletId(), request.getServiceId(), request.getPackageId());
    }

    private CatalogContext resolveCatalogContext(UUID outletId, UUID serviceId, UUID packageId) {
        if (outletId == null) {
            throw new BusinessException("Outlet is required");
        }
        if ((serviceId == null) == (packageId == null)) {
            throw new BusinessException("Select exactly one service or package");
        }

        Outlet outlet = outletRepository.findById(outletId)
                .filter(o -> !o.isDeleted() && o.isActive())
                .orElseThrow(() -> new BusinessException("Outlet is unavailable"));
        if (serviceId != null) {
            SalonService service = salonServiceRepository.findById(serviceId)
                    .filter(s -> !s.isDeleted() && s.isActive())
                    .orElseThrow(() -> new BusinessException("Service is unavailable"));
            validateCatalogOutlet(service.getOutlet(), outlet, "Service");
            return new CatalogContext(outlet, service, null, service.getPrice());
        }

        ServicePackage servicePackage = servicePackageRepository.findById(packageId)
                .filter(p -> !p.isDeleted() && p.isActive())
                .orElseThrow(() -> new BusinessException("Package is unavailable"));
        validateCatalogOutlet(servicePackage.getOutlet(), outlet, "Package");
        // Package.price is the authoritative subtotal; discountPct is intentionally not applied here.
        return new CatalogContext(outlet, null, servicePackage, servicePackage.getPrice());
    }

    private void validateCatalogOutlet(Outlet catalogOutlet, Outlet selectedOutlet, String itemType) {
        if (catalogOutlet != null && !catalogOutlet.getId().equals(selectedOutlet.getId())) {
            throw new BusinessException(itemType + " is not available at the selected outlet");
        }
    }

    private CouponScope resolveAdminScope(CouponRequest request) {
        if (request.getServiceId() != null && request.getPackageId() != null) {
            throw new BusinessException("Coupon can target either a service or a package, not both");
        }

        Outlet outlet = request.getOutletId() == null ? null : findScopeOutlet(request.getOutletId());
        SalonService service = request.getServiceId() == null ? null : findScopeService(request.getServiceId());
        ServicePackage servicePackage = request.getPackageId() == null
                ? null : findScopePackage(request.getPackageId());

        if (outlet != null && service != null) {
            validateScopeOutlet(service.getOutlet(), outlet, "Service");
        }
        if (outlet != null && servicePackage != null) {
            validateScopeOutlet(servicePackage.getOutlet(), outlet, "Package");
        }
        return new CouponScope(outlet, service, servicePackage);
    }

    private void validateScopeOutlet(Outlet itemOutlet, Outlet couponOutlet, String itemType) {
        if (itemOutlet != null && !itemOutlet.getId().equals(couponOutlet.getId())) {
            throw new BusinessException(itemType + " scope does not belong to the coupon outlet");
        }
    }

    private void validateCouponRequest(CouponRequest request) {
        if (request.getValidFrom() == null || request.getValidUntil() == null
                || !request.getValidUntil().isAfter(request.getValidFrom())) {
            throw new BusinessException("Coupon valid-until must be after valid-from");
        }
        if (request.getDiscountType() == CouponDiscountType.PERCENTAGE
                && request.getDiscountValue() != null
                && request.getDiscountValue().compareTo(ONE_HUNDRED) > 0) {
            throw new BusinessException("Percentage discount cannot exceed 100");
        }
    }

    private void ensureBookingOwnershipAndContext(
            Booking booking, User customer, CouponApplication application) {
        if (!booking.getCustomer().getId().equals(customer.getId())) {
            throw new BusinessException("Booking does not belong to the customer");
        }
        if (!booking.getOutlet().getId().equals(application.outletId())) {
            throw new BusinessException("Prepared coupon outlet does not match booking");
        }
        UUID bookingServiceId = booking.getService() == null ? null : booking.getService().getId();
        UUID bookingPackageId = booking.getServicePackage() == null ? null : booking.getServicePackage().getId();
        if (!Objects.equals(bookingServiceId, application.serviceId())
                || !Objects.equals(bookingPackageId, application.packageId())) {
            throw new BusinessException("Prepared coupon item does not match booking");
        }
        if (booking.getCoupon() == null
                || !booking.getCoupon().getId().equals(application.couponId())
                || !Objects.equals(booking.getCouponCode(), application.couponCode())
                || booking.getCouponDiscountType() != application.discountType()
                || !sameAmount(booking.getCouponDiscountValue(), application.discountValue())
                || !sameNullableAmount(
                        booking.getCouponMaximumDiscount(), application.maximumDiscount())
                || !sameAmount(booking.getSubtotalAmount(), application.subtotalAmount())
                || !sameAmount(booking.getDiscountAmount(), application.discountAmount())
                || !sameAmount(booking.getTotalAmount(), application.totalAmount())) {
            throw new BusinessException("Prepared coupon pricing does not match booking snapshot");
        }
    }

    private void ensureSameReservation(CouponRedemption redemption, Coupon coupon, User customer) {
        if (!redemption.getCoupon().getId().equals(coupon.getId())
                || !redemption.getCustomer().getId().equals(customer.getId())) {
            throw new BusinessException("Booking already has a different coupon reservation");
        }
    }

    private void ensureApplicationUnchanged(CouponApplication prepared, CouponApplication current) {
        boolean unchanged = Objects.equals(prepared.couponId(), current.couponId())
                && Objects.equals(prepared.couponCode(), current.couponCode())
                && prepared.discountType() == current.discountType()
                && sameAmount(prepared.discountValue(), current.discountValue())
                && sameNullableAmount(prepared.maximumDiscount(), current.maximumDiscount())
                && sameAmount(prepared.subtotalAmount(), current.subtotalAmount())
                && sameAmount(prepared.discountAmount(), current.discountAmount())
                && sameAmount(prepared.totalAmount(), current.totalAmount())
                && Objects.equals(prepared.outletId(), current.outletId())
                && Objects.equals(prepared.serviceId(), current.serviceId())
                && Objects.equals(prepared.packageId(), current.packageId());
        if (!unchanged) {
            throw new BusinessException("Coupon or catalog pricing changed; prepare the application again");
        }
    }

    private boolean sameAmount(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.compareTo(right) == 0;
    }

    private boolean sameNullableAmount(BigDecimal left, BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }

    private Coupon saveCoupon(Coupon coupon) {
        try {
            return couponRepository.saveAndFlush(coupon);
        } catch (DataIntegrityViolationException ex) {
            if (isNormalizedCodeConflict(ex)) {
                throw new BusinessException("Coupon code already exists");
            }
            throw ex;
        }
    }

    private boolean isNormalizedCodeConflict(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof org.hibernate.exception.ConstraintViolationException constraintViolation
                    && constraintViolation.getConstraintName() != null
                    && constraintViolation.getConstraintName().toLowerCase(Locale.ROOT)
                    .contains("normalized_code")) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("normalized_code")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private User findCustomer(UUID id) {
        User user = userRepository.findById(id)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
        if (!user.isActive() || user.getRole() != UserRole.CUSTOMER) {
            throw new BusinessException("Customer account is unavailable");
        }
        return user;
    }

    private Booking findBooking(UUID id) {
        return bookingRepository.findById(id)
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));
    }

    private Coupon findCoupon(UUID id) {
        return couponRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
    }

    private Outlet findScopeOutlet(UUID id) {
        return outletRepository.findById(id)
                .filter(o -> !o.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Outlet", "id", id));
    }

    private SalonService findScopeService(UUID id) {
        return salonServiceRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", id));
    }

    private ServicePackage findScopePackage(UUID id) {
        return servicePackageRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Package", "id", id));
    }

    private CouponResponse toResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .name(coupon.getName())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minimumSpend(coupon.getMinimumSpend())
                .maximumDiscount(coupon.getMaximumDiscount())
                .validFrom(coupon.getValidFrom())
                .validUntil(coupon.getValidUntil())
                .usageLimit(coupon.getUsageLimit())
                .perCustomerLimit(coupon.getPerCustomerLimit())
                .reservedCount(couponRedemptionRepository.countByCouponIdAndStatusAndIsDeletedFalse(
                        coupon.getId(), CouponRedemptionStatus.RESERVED))
                .redeemedCount(couponRedemptionRepository.countByCouponIdAndStatusAndIsDeletedFalse(
                        coupon.getId(), CouponRedemptionStatus.REDEEMED))
                .outletId(coupon.getOutlet() == null ? null : coupon.getOutlet().getId())
                .serviceId(coupon.getService() == null ? null : coupon.getService().getId())
                .packageId(coupon.getServicePackage() == null ? null : coupon.getServicePackage().getId())
                .isActive(coupon.isActive())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .build();
    }

    private CouponValidationResponse toValidationResponse(Coupon coupon, CouponApplication application) {
        return CouponValidationResponse.builder()
                .couponId(application.couponId())
                .code(application.couponCode())
                .discountType(application.discountType())
                .discountValue(application.discountValue())
                .maximumDiscount(application.maximumDiscount())
                .subtotalAmount(application.subtotalAmount())
                .discountAmount(application.discountAmount())
                .totalAmount(application.totalAmount())
                .validUntil(coupon.getValidUntil())
                .build();
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("Coupon code is required");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Booking coupon reservation released";
        }
        String trimmed = reason.trim();
        return trimmed.length() <= 500 ? trimmed : trimmed.substring(0, 500);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nullableMoney(BigDecimal value) {
        return value == null ? null : money(value);
    }

    private record CouponScope(Outlet outlet, SalonService service, ServicePackage servicePackage) {
    }

    private record CatalogContext(
            Outlet outlet, SalonService service, ServicePackage servicePackage, BigDecimal subtotal) {
    }
}
