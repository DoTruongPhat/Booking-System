package com.booking.presentation.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
public class BookingResponse {

    private UUID id;
    private String bookingCode;
    private UUID userId;
    private UUID roomId;
    private UUID hotelId;

    // Dates
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private int numNights;

    // Guests
    private int numGuests;
    private int numRooms;

    // Price
    private BigDecimal unitPrice;
    private BigDecimal discountAmount;
    private BigDecimal totalPrice;

    // Status
    private String status;
    private String paymentStatus;
    private String paymentMethod;
    private Instant paidAt;
    private BigDecimal refundAmount;

    // Cancellation
    private String cancellationReason;
    private Instant cancelledAt;
    private String cancelledBy;

    // Extra
    private String specialRequest;

    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
}