package com.salon.app.module.booking.service;

import com.salon.app.module.booking.dto.request.ApproveBookingRequest;
import com.salon.app.module.booking.dto.request.CreateBookingRequest;
import com.salon.app.module.booking.dto.request.RescheduleBookingRequest;
import com.salon.app.module.booking.dto.response.BookingResponse;
import com.salon.app.shared.enums.BookingStatus;
import com.salon.app.shared.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface BookingService {
    BookingResponse createBooking(UUID customerId, CreateBookingRequest request);
    PagedResponse<BookingResponse> getMyBookings(UUID customerId, Pageable pageable);
    PagedResponse<BookingResponse> getBookings(UUID outletId, LocalDate date, BookingStatus status, Pageable pageable);
    BookingResponse approveBooking(UUID bookingId, ApproveBookingRequest request);
    BookingResponse rejectBooking(UUID bookingId, String reason);
    BookingResponse completeBooking(UUID bookingId);
    BookingResponse cancelBooking(UUID bookingId, UUID customerId);
    BookingResponse rescheduleBooking(UUID bookingId, UUID customerId, RescheduleBookingRequest request);
}
