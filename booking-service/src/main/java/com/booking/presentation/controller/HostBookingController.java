package com.booking.presentation.controller;

import com.booking.application.port.in.QueryBookingUseCase;
import com.booking.application.port.in.QueryHotelUseCase;
import com.booking.domain.exception.CoreErrorCode;
import com.booking.domain.exception.CoreException;
import com.booking.domain.model.Hotel;
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
@RequestMapping("/api/host/bookings")
@RequiredArgsConstructor
public class HostBookingController {

    private final QueryBookingUseCase queryBookingUseCase;
    private final QueryHotelUseCase queryHotelUseCase;
    private final BookingDtoMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getBookingsByHotel(
            @RequestParam UUID hotelId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10) Pageable pageable) {

        // Verify host owns this hotel
        UUID ownerUserId = SecurityUtils.getCurrentUserId();
        Hotel hotel = queryHotelUseCase.getById(hotelId);
        if (!hotel.isOwnedBy(ownerUserId)) {
            throw new CoreException(CoreErrorCode.HOTEL_NOT_OWNED);
        }

        Page<BookingResponse> bookings = queryBookingUseCase
                .getByHotelId(hotelId, status, pageable)
                .map(mapper::toResponse);

        return ResponseEntity.ok(ApiResponse.success(bookings));
    }
}
