package com.booking.application.port.in;

import com.booking.domain.enums.CancelledBy;
import com.booking.domain.model.Booking;

import java.util.UUID;

public interface CancelBookingUseCase {

    Booking cancelBooking(UUID bookingId, UUID requesterId, CancelledBy cancelledBy, String reason);
}