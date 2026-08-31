package com.salon.app.module.coupon.service;

import com.salon.app.module.coupon.dto.request.CouponRequest;
import com.salon.app.module.coupon.dto.request.CouponValidationRequest;
import com.salon.app.module.coupon.dto.response.CouponApplication;
import com.salon.app.module.coupon.dto.response.CouponResponse;
import com.salon.app.module.coupon.dto.response.CouponValidationResponse;

import java.util.List;
import java.util.UUID;

public interface CouponService {
    List<CouponResponse> getAllCoupons();
    CouponResponse getCouponById(UUID id);
    CouponResponse createCoupon(CouponRequest request);
    CouponResponse updateCoupon(UUID id, CouponRequest request);
    CouponResponse toggleCoupon(UUID id);
    void deleteCoupon(UUID id);

    /** Advisory customer-facing validation; booking integration must use prepareApplication and reserve. */
    CouponValidationResponse validateCoupon(UUID customerId, CouponValidationRequest request);

    /** Produces a trusted, immutable pricing snapshot from the current catalog and coupon state. */
    CouponApplication prepareApplication(UUID customerId, CouponValidationRequest request);

    /** Idempotently reserves one prepared coupon application for a booking. */
    void reserve(UUID customerId, UUID bookingId, CouponApplication application);

    /** Idempotently transitions a booking's coupon reservation to redeemed. */
    void redeem(UUID bookingId);

    /** Idempotently releases a booking's unredeemed coupon reservation. */
    void release(UUID bookingId, String reason);
}
