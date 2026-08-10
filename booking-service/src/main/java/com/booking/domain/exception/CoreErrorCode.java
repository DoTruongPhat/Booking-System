package com.booking.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CoreErrorCode {

    // ─── Hotel ───────────────────────────────
    HOTEL_NOT_FOUND("CORE_001", "Hotel not found", HttpStatus.NOT_FOUND),
    HOTEL_NOT_OWNED("CORE_002", "You do not own this hotel", HttpStatus.FORBIDDEN),
    HOTEL_NOT_ACTIVE("CORE_003", "Hotel is not active", HttpStatus.BAD_REQUEST),
    HOTEL_ALREADY_APPROVED("CORE_004", "Hotel is already approved", HttpStatus.BAD_REQUEST),
    HOTEL_NAME_DUPLICATE("CORE_005", "Hotel name already exists in this city", HttpStatus.CONFLICT),

    // ─── Room ────────────────────────────────
    ROOM_NOT_FOUND("CORE_010", "Room not found", HttpStatus.NOT_FOUND),
    ROOM_NOT_OWNED("CORE_011", "You do not own this room's hotel", HttpStatus.FORBIDDEN),
    ROOM_NOT_AVAILABLE("CORE_012", "Room is not available", HttpStatus.BAD_REQUEST),
    ROOM_TYPE_NOT_FOUND("CORE_013", "Room type not found", HttpStatus.NOT_FOUND),
    ROOM_TYPE_DUPLICATE("CORE_014", "Room type code already exists", HttpStatus.CONFLICT),

    // ─── Availability ────────────────────────
    AVAILABILITY_NOT_FOUND("CORE_020", "Room availability not found for date range", HttpStatus.NOT_FOUND),
    AVAILABILITY_INSUFFICIENT("CORE_021", "Not enough rooms available for the requested dates", HttpStatus.CONFLICT),

    // ─── Booking ─────────────────────────────
    BOOKING_NOT_FOUND("CORE_030", "Booking not found", HttpStatus.NOT_FOUND),
    BOOKING_NOT_OWNED("CORE_031", "You do not own this booking", HttpStatus.FORBIDDEN),
    BOOKING_ALREADY_CANCELLED("CORE_032", "Booking is already cancelled", HttpStatus.BAD_REQUEST),
    BOOKING_CANNOT_CANCEL("CORE_033", "Booking cannot be cancelled in current status", HttpStatus.BAD_REQUEST),
    BOOKING_INVALID_DATES("CORE_034", "Check-out date must be after check-in date", HttpStatus.BAD_REQUEST),
    BOOKING_PAST_CHECKIN("CORE_035", "Check-in date cannot be in the past", HttpStatus.BAD_REQUEST),
    BOOKING_TOO_FAR_AHEAD("CORE_036", "Check-in date cannot be more than 365 days ahead", HttpStatus.BAD_REQUEST),
    BOOKING_GUEST_EXCEEDS_CAPACITY("CORE_037", "Number of guests exceeds room capacity", HttpStatus.BAD_REQUEST),
    BOOKING_DUPLICATE("CORE_038", "Duplicate booking for same room with overlapping dates", HttpStatus.CONFLICT),
    BOOKING_INVALID_STATUS("CORE_039", "Booking status does not allow this action", HttpStatus.BAD_REQUEST),

    // ─── Marketing ─────────────────────────────────────────
    PROMOTION_NOT_FOUND("CORE_050", "Promotion not found", HttpStatus.NOT_FOUND),
    VOUCHER_NOT_FOUND("CORE_060", "Voucher not found", HttpStatus.NOT_FOUND),
    VOUCHER_CODE_DUPLICATE("CORE_061", "Voucher code already exists", HttpStatus.CONFLICT),


    // ─── General ─────────────────────────────
    INVALID_REQUEST("CORE_900", "Invalid request", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("CORE_999", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
