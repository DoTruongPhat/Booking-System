package com.booking.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public sealed interface CoreDomainEvent {

    Instant occurredAt();

    // ─── Hotel Events ────────────────────────

    record HotelCreated(
            UUID hotelId,
            UUID ownerUserId,
            String name,
            String city,
            String hostEmail,
            Instant occurredAt
    ) implements CoreDomainEvent {}

    record HotelApproved(
            UUID hotelId,
            String name,
            Instant occurredAt
    ) implements CoreDomainEvent {}

    record HotelDeactivated(
            UUID hotelId,
            String name,
            Instant occurredAt
    ) implements CoreDomainEvent {}

    record HotelChangeRequested(
            UUID changeRequestId,
            UUID hotelId,
            UUID ownerUserId,
            String hotelName,
            String city,
            String hostEmail,
            Object proposedChanges,
            Instant occurredAt
    ) implements CoreDomainEvent {}

    // ─── Booking Events ─────────────────────

    record BookingCreated(
            UUID bookingId,
            String bookingCode,
            UUID userId,
            UUID roomId,
            UUID hotelId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int numRooms,
            BigDecimal totalPrice,
            Instant occurredAt
    ) implements CoreDomainEvent {}

    record BookingConfirmed(
            UUID bookingId,
            String bookingCode,
            Instant occurredAt
    ) implements CoreDomainEvent {}

    record BookingCancelled(
            UUID bookingId,
            String bookingCode,
            UUID userId,
            UUID roomId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int numRooms,
            BigDecimal refundAmount,
            String cancelledBy,
            Instant occurredAt
    ) implements CoreDomainEvent {}

    // ─── Availability Events ────────────────

    record RoomAvailabilityChanged(
            UUID roomId,
            LocalDate date,
            int availableCount,
            String status,
            Instant occurredAt
    ) implements CoreDomainEvent {}
}
