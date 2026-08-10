package com.booking.presentation.controller;

import com.booking.application.port.in.CancelBookingUseCase;
import com.booking.application.port.in.ManageStayUseCase;
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
    private final ManageStayUseCase manageStayUseCase;
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

    @PostMapping("/{id}/check-in")
    public ResponseEntity<ApiResponse<BookingResponse>> checkIn(@PathVariable UUID id) {
        Booking checkedIn = manageStayUseCase.checkIn(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Booking checked in", mapper.toResponse(checkedIn)));
    }

    @PostMapping("/{id}/check-out")
    public ResponseEntity<ApiResponse<BookingResponse>> checkOut(@PathVariable UUID id) {
        Booking checkedOut = manageStayUseCase.checkOut(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Booking checked out", mapper.toResponse(checkedOut)));
    }

    @PostMapping("/{id}/no-show")
    public ResponseEntity<ApiResponse<BookingResponse>> noShow(
            @PathVariable UUID id,
            @RequestBody(required = false) NoShowRequest request) {

        String reason = request != null ? request.getReason() : null;
        Booking noShow = manageStayUseCase.markNoShow(id, SecurityUtils.getCurrentUserId(), reason);
        return ResponseEntity.ok(ApiResponse.success("Booking marked no-show", mapper.toResponse(noShow)));
    }

    public static class ForceCancelRequest {
        @NotBlank(message = "Reason is required for force cancellation")
        private String reason;

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class NoShowRequest {
        private String reason;

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
