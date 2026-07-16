package com.booking.presentation.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class CreateBookingRequest {

    @NotNull(message = "Room ID is required")
    private UUID roomId;

    @NotNull(message = "Check-in date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkInDate;

    @NotNull(message = "Check-out date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkOutDate;

    @NotNull(message = "Number of guests is required")
    @Min(value = 1, message = "At least 1 guest")
    private Integer numGuests;

    @Min(value = 1, message = "At least 1 room")
    private Integer numRooms = 1;

    private String specialRequest;
    @NotNull(message = "Guest name is required")
    private String guestName;

    @NotNull(message = "Guest email is required")
    private String guestEmail;

    private String guestPhone;
}