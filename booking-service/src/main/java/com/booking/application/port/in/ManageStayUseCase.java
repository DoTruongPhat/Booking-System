package com.booking.application.port.in;

import com.booking.domain.model.Booking;

import java.util.UUID;

public interface ManageStayUseCase {
    Booking checkIn(UUID bookingId, UUID hostUserId);
    Booking checkOut(UUID bookingId, UUID hostUserId);
    Booking markNoShow(UUID bookingId, UUID hostUserId, String reason);
}
