package com.booking.application.port.in;

import com.booking.domain.model.Booking;

import java.time.LocalDate;
import java.util.UUID;

public interface CreateBookingUseCase {

    Booking createBooking(BookingCommand command, UUID userId);

    record BookingCommand(
            UUID roomId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int numGuests,
            int numRooms,
            String specialRequest,
            String guestName,
            String guestEmail,
            String guestPhone,
            String voucherCode
    ) {}

}
