package com.booking.domain.event;

import java.time.Instant;
import java.util.UUID;

public record BookingConfirmedEvent(
        UUID eventId,
        UUID bookingId,
        String bookingCode,
        String guestEmail,
        Instant occurredAt
) {
    public static BookingConfirmedEvent of(UUID bookingId, String bookingCode, String guestEmail) {
        return new BookingConfirmedEvent(
                UUID.randomUUID(), bookingId, bookingCode, guestEmail, Instant.now());
    }
}