package com.booking.presentation.controller;

import com.booking.application.port.in.ConfirmBookingUseCase;
import com.booking.application.port.in.QueryBookingUseCase;
import com.booking.presentation.mapper.BookingDtoMapper;
import com.booking.presentation.response.ApiResponse;
import com.booking.presentation.response.BookingResponse;
import com.booking.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {

    private final QueryBookingUseCase queryBookingUseCase;
    private final BookingDtoMapper mapper;
    private final ConfirmBookingUseCase confirmBookingUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getAllBookings(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<BookingResponse> bookings = queryBookingUseCase
                .getAll(status, pageable)
                .map(mapper::toResponse);

        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(@PathVariable UUID id) {
        var booking = queryBookingUseCase.getById(id);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(booking)));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(@PathVariable UUID id) {
        UUID adminUserId = SecurityUtils.getCurrentUserId();
        var confirmed = confirmBookingUseCase.confirmBooking(id, adminUserId);
        return ResponseEntity.ok(ApiResponse.success("Booking confirmed", mapper.toResponse(confirmed)));
    }
}