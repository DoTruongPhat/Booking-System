package com.booking.presentation.controller;

import com.booking.application.port.in.CancelBookingUseCase;
import com.booking.application.port.in.CreateBookingUseCase;
import com.booking.application.port.in.QueryBookingUseCase;
import com.booking.domain.enums.CancelledBy;
import com.booking.domain.exception.CoreErrorCode;
import com.booking.domain.exception.CoreException;
import com.booking.domain.model.Booking;
import com.booking.presentation.mapper.BookingDtoMapper;
import com.booking.presentation.request.CancelBookingRequest;
import com.booking.presentation.request.CreateBookingRequest;
import com.booking.presentation.response.ApiResponse;
import com.booking.presentation.response.BookingResponse;
import com.booking.shared.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/user/bookings")
@RequiredArgsConstructor
public class UserBookingController {

    private final CreateBookingUseCase createBookingUseCase;
    private final CancelBookingUseCase cancelBookingUseCase;
    private final QueryBookingUseCase queryBookingUseCase;
    private final BookingDtoMapper mapper;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request) {

        UUID userId = SecurityUtils.getCurrentUserId();
        Booking booking = createBookingUseCase.createBooking(
                mapper.toCommand(request), userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(mapper.toResponse(booking)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable UUID id,
            @RequestBody(required = false) CancelBookingRequest request) {

        UUID userId = SecurityUtils.getCurrentUserId();
        String reason = request != null ? request.getReason() : null;

        Booking cancelled = cancelBookingUseCase.cancelBooking(
                id, userId, CancelledBy.USER, reason);

        return ResponseEntity.ok(ApiResponse.success("Booking cancelled", mapper.toResponse(cancelled)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getMyBookings(
            @PageableDefault(size = 10) Pageable pageable) {

        UUID userId = SecurityUtils.getCurrentUserId();
        Page<BookingResponse> bookings = queryBookingUseCase
                .getByUserId(userId, pageable)
                .map(mapper::toResponse);

        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Booking booking = queryBookingUseCase.getById(id);
        if (!userId.equals(booking.getUserId())) {
            throw new CoreException(CoreErrorCode.BOOKING_NOT_OWNED);
        }
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(booking)));
    }
}
