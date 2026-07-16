package com.booking.application.port.in;

import com.booking.domain.model.Booking;

import java.util.UUID;

public interface ConfirmBookingUseCase {
    Booking confirmBooking(UUID bookingId, UUID confirmedByUserId);
}