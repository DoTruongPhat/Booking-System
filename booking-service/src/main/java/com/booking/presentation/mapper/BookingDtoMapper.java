package com.booking.presentation.mapper;

import com.booking.application.port.in.CreateBookingUseCase.BookingCommand;
import com.booking.domain.model.Booking;
import com.booking.presentation.request.CreateBookingRequest;
import com.booking.presentation.response.BookingResponse;
import org.springframework.stereotype.Component;

@Component
public class BookingDtoMapper {

    public BookingCommand toCommand(CreateBookingRequest request) {
        return new BookingCommand(
                request.getRoomId(),
                request.getCheckInDate(),
                request.getCheckOutDate(),
                request.getNumGuests(),
                request.getNumRooms(),
                request.getSpecialRequest(),
                request.getGuestName(),
                request.getGuestEmail(),
                request.getGuestPhone()
        );
    }

    public BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())
                .userId(booking.getUserId())
                .roomId(booking.getRoomId())
                .hotelId(booking.getHotelId())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .numNights(booking.getNumNights())
                .numGuests(booking.getNumGuests())
                .numRooms(booking.getNumRooms())
                .unitPrice(booking.getUnitPrice())
                .discountAmount(booking.getDiscountAmount())
                .totalPrice(booking.getTotalPrice())
                .status(booking.getStatus().name())
                .paymentStatus(booking.getPaymentStatus().name())
                .paymentMethod(booking.getPaymentMethod())
                .paidAt(booking.getPaidAt())
                .refundAmount(booking.getRefundAmount())
                .cancellationReason(booking.getCancellationReason())
                .cancelledAt(booking.getCancelledAt())
                .cancelledBy(booking.getCancelledBy() != null ? booking.getCancelledBy().name() : null)
                .specialRequest(booking.getSpecialRequest())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}