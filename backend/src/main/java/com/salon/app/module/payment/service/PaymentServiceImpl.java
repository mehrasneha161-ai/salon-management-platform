package com.salon.app.module.payment.service;

import com.salon.app.module.booking.entity.Booking;
import com.salon.app.module.booking.event.BookingConfirmedEvent;
import com.salon.app.module.booking.repository.BookingRepository;
import com.salon.app.module.payment.dto.request.InitiatePaymentRequest;
import com.salon.app.module.payment.dto.request.VerifyPaymentRequest;
import com.salon.app.module.payment.dto.response.PaymentResponse;
import com.salon.app.module.payment.entity.Payment;
import com.salon.app.module.payment.repository.PaymentRepository;
import com.salon.app.shared.enums.BookingStatus;
import com.salon.app.shared.enums.PaymentStatus;
import com.salon.app.shared.exception.BusinessException;
import com.salon.app.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${payment.razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${payment.razorpay.key-secret:}")
    private String razorpayKeySecret;

    @Override
    @Transactional
    public PaymentResponse initiate(UUID customerId, InitiatePaymentRequest request) {
        log.info("Initiating payment for booking: {}", request.getBookingId());
        Booking booking = bookingRepository.findById(request.getBookingId())
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", request.getBookingId()));
        if (!booking.getCustomer().getId().equals(customerId)) {
            throw new BusinessException("You can only pay for your own bookings");
        }
        if (booking.getStatus() != BookingStatus.SLOT_LOCKED) {
            throw new BusinessException("This booking is not awaiting payment");
        }

        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new BusinessException("This booking is already paid");
        }
        if (payment == null) {
            payment = Payment.builder()
                    .booking(booking)
                    .amount(booking.getTotalAmount())
                    .currency("INR")
                    .gateway("razorpay")
                    .status(PaymentStatus.PENDING)
                    .build();
        }
        // A gateway order reference. With the Razorpay SDK this would be the real
        // order id; here we generate a stable reference for the checkout.
        if (payment.getGatewayTxnId() == null) {
            payment.setGatewayTxnId("order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        }
        paymentRepository.save(payment);
        return toResponse(payment, true);
    }

    @Override
    @Transactional
    public PaymentResponse verify(UUID customerId, VerifyPaymentRequest request) {
        log.info("Verifying payment: {}", request.getPaymentId());
        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", request.getPaymentId()));
        if (!payment.getBooking().getCustomer().getId().equals(customerId)) {
            throw new BusinessException("You can only verify your own payments");
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return toResponse(payment, false); // idempotent
        }
        if (!signatureValid(payment.getGatewayTxnId(), request.getGatewayPaymentId(), request.getGatewaySignature())) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new BusinessException("Payment signature verification failed");
        }
        markPaidAndConfirm(payment, request.getGatewayPaymentId());
        return toResponse(payment, false);
    }

    @Override
    public PaymentResponse getByBooking(UUID bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "bookingId", bookingId));
        return toResponse(payment, false);
    }

    @Override
    @Transactional
    public void handleWebhook(Map<String, Object> payload) {
        // Simplified webhook: look up by our order reference and mark paid.
        // (A production integration would verify the X-Razorpay-Signature header.)
        String orderId = str(payload.get("razorpay_order_id"));
        String paymentId = str(payload.get("razorpay_payment_id"));
        if (orderId == null) {
            log.warn("Webhook missing razorpay_order_id; ignoring");
            return;
        }
        paymentRepository.findByGatewayTxnId(orderId).ifPresent(payment -> {
            if (payment.getStatus() != PaymentStatus.SUCCESS) {
                markPaidAndConfirm(payment, paymentId);
                log.info("Webhook marked payment {} as SUCCESS", payment.getId());
            }
        });
    }

    // ---------------------------------------------------------------- helpers

    private void markPaidAndConfirm(Payment payment, String gatewayPaymentId) {
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(Instant.now());
        if (gatewayPaymentId != null && !gatewayPaymentId.isBlank()) {
            payment.setGatewayTxnId(gatewayPaymentId);
        }
        paymentRepository.save(payment);

        Booking booking = payment.getBooking();
        booking.setStatus(BookingStatus.CONFIRMED);
        // Initialise lazy associations in-transaction for the async handlers.
        booking.getCustomer().getFullName();
        booking.getCustomer().getEmail();
        booking.getCustomer().getPhoneNumber();
        booking.getOutlet().getName();
        bookingRepository.save(booking);
        eventPublisher.publishEvent(new BookingConfirmedEvent(this, booking));
        log.info("Payment success -> booking {} CONFIRMED", booking.getBookingRef());
    }

    /**
     * Verifies the Razorpay checkout signature (HMAC-SHA256 of "orderId|paymentId").
     * In dev (no key secret configured) verification is skipped so the flow is testable.
     */
    private boolean signatureValid(String orderId, String paymentId, String signature) {
        if (razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            return true; // dev mode — no real gateway configured
        }
        if (paymentId == null || signature == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).equals(signature);
        } catch (Exception ex) {
            log.warn("Signature verification error: {}", ex.getMessage());
            return false;
        }
    }

    private String str(Object o) {
        return o == null ? null : o.toString();
    }

    private PaymentResponse toResponse(Payment p, boolean includeKey) {
        return PaymentResponse.builder()
                .id(p.getId())
                .bookingId(p.getBooking().getId())
                .bookingRef(p.getBooking().getBookingRef())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .gateway(p.getGateway())
                .status(p.getStatus().name())
                .orderRef(p.getGatewayTxnId())
                .keyId(includeKey ? razorpayKeyId : null)
                .paidAt(p.getPaidAt())
                .build();
    }
}
