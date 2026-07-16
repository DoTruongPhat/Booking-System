package com.booking.presentation.controller;

import com.booking.application.port.in.CancelBookingUseCase;
import com.booking.application.port.in.QueryBookingUseCase;
import com.booking.application.port.in.QueryHotelUseCase;
import com.booking.domain.enums.CancelledBy;
import com.booking.domain.exception.CoreErrorCode;
import com.booking.domain.exception.CoreException;
import com.booking.domain.model.Booking;
import com.booking.domain.model.Hotel;
import com.booking.presentation.mapper.BookingDtoMapper;
import com.booking.presentation.request.CancelBookingRequest;
import com.booking.presentation.response.ApiResponse;
import com.booking.presentation.response.BookingResponse;
import com.booking.shared.util.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/host/bookings")
@RequiredArgsConstructor
public class HostBookingActionController {

    private final CancelBookingUseCase cancelBookingUseCase;
    private final QueryBookingUseCase queryBookingUseCase;
    private final QueryHotelUseCase queryHotelUseCase;
    private final BookingDtoMapper mapper;

    /**
     * BR-CANCEL-011: HOST can force cancel a booking of their own hotel.
     * Reason is required to justify the action (compensates guest with 100% refund).
     */
    @PostMapping("/{id}/force-cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> forceCancelBooking(
            @PathVariable UUID id,
            @Valid @RequestBody ForceCancelRequest request) {

        UUID ownerUserId = SecurityUtils.getCurrentUserId();

        Booking booking = queryBookingUseCase.getById(id);
        Hotel hotel = queryHotelUseCase.getById(booking.getHotelId());

        if (!hotel.isOwnedBy(ownerUserId)) {
            throw new CoreException(CoreErrorCode.HOTEL_NOT_OWNED);
        }

        Booking cancelled = cancelBookingUseCase.cancelBooking(
                id, ownerUserId, CancelledBy.HOST, request.getReason());

        return ResponseEntity.ok(ApiResponse.success("Booking force-cancelled", mapper.toResponse(cancelled)));
    }

    public static class ForceCancelRequest {
        @NotBlank(message = "Reason is required for force cancellation")
        private String reason;

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}