package com.salon.app.module.booking.controller;

import com.salon.app.module.booking.dto.request.ApproveBookingRequest;
import com.salon.app.module.booking.dto.request.CreateBookingRequest;
import com.salon.app.module.booking.dto.request.RescheduleBookingRequest;
import com.salon.app.module.booking.dto.response.BookingResponse;
import com.salon.app.module.booking.service.BookingService;
import com.salon.app.module.booking.service.SlotAvailabilityService;
import com.salon.app.module.auth.repository.UserRepository;
import com.salon.app.shared.enums.BookingStatus;
import com.salon.app.shared.response.ApiResponse;
import com.salon.app.shared.response.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final SlotAvailabilityService slotAvailabilityService;
    private final UserRepository userRepository;

    @PostMapping("/bookings")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID customerId = resolveUserId(userDetails);
        BookingResponse response = bookingService.createBooking(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created. Slot locked for 10 minutes.", response));
    }

    @GetMapping("/bookings/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PagedResponse<BookingResponse>>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID customerId = resolveUserId(userDetails);
        PagedResponse<BookingResponse> response = bookingService.getMyBookings(
                customerId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<BookingResponse>>> getBookings(
            @RequestParam(required = false) UUID outletId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<BookingResponse> response = bookingService.getBookings(
                outletId, date, status, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/bookings/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> approveBooking(
            @PathVariable UUID id, @Valid @RequestBody ApproveBookingRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Booking approved", bookingService.approveBooking(id, request)));
    }

    @PutMapping("/bookings/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> rejectBooking(
            @PathVariable UUID id, @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(ApiResponse.success("Booking rejected", bookingService.rejectBooking(id, reason)));
    }

    @PutMapping("/bookings/{id}/complete")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> completeBooking(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Booking completed", bookingService.completeBooking(id)));
    }

    @DeleteMapping("/bookings/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        UUID customerId = resolveUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled", bookingService.cancelBooking(id, customerId)));
    }

    @PutMapping("/bookings/{id}/reschedule")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingResponse>> rescheduleBooking(
            @PathVariable UUID id,
            @Valid @RequestBody RescheduleBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID customerId = resolveUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                "Booking rescheduled. Slot locked for 10 minutes.",
                bookingService.rescheduleBooking(id, customerId, request)));
    }

    @GetMapping("/bookings/assigned")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getAssignedBookings(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.getAssignedBookings(resolveUserId(userDetails), date)));
    }

    @GetMapping("/slots/available")
    public ResponseEntity<ApiResponse<List<String>>> getAvailableSlots(
            @RequestParam UUID outletId,
            @RequestParam UUID staffId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "30") int durationMinutes) {
        List<String> slots = slotAvailabilityService.getAvailableSlots(outletId, staffId, date, durationMinutes);
        return ResponseEntity.ok(ApiResponse.success(slots));
    }

    private UUID resolveUserId(UserDetails userDetails) {
        return userRepository.findByPhoneNumberAndIsDeletedFalse(userDetails.getUsername())
                .orElseThrow().getId();
    }
}
