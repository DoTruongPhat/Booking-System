package com.booking.presentation.mapper;

import com.booking.application.port.in.CreateBookingUseCase.BookingCommand;
import com.booking.application.port.out.HotelRepositoryPort;
import com.booking.application.port.out.RoomRepositoryPort;
import com.booking.application.service.PaymentDeadlinePolicy;
import com.booking.domain.model.Booking;
import com.booking.domain.model.Hotel;
import com.booking.domain.model.Room;
import com.booking.presentation.request.CreateBookingRequest;
import com.booking.presentation.response.BookingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BookingDtoMapper {

    private final RoomRepositoryPort roomRepositoryPort;
    private final HotelRepositoryPort hotelRepositoryPort;
    private final PaymentDeadlinePolicy paymentDeadlinePolicy;

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
                request.getGuestPhone(),
                request.getVoucherCode()
        );
    }

    public BookingResponse toResponse(Booking booking) {
        Optional<Room> room = roomRepositoryPort.findById(booking.getRoomId());
        Optional<Hotel> hotel = hotelRepositoryPort.findById(booking.getHotelId());

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())
                .userId(booking.getUserId())
                .roomId(booking.getRoomId())
                .hotelId(booking.getHotelId())
                .roomName(room.map(Room::getName).orElse(null))
                .hotelName(hotel.map(Hotel::getName).orElse(null))
                .hotelAddress(hotel.map(Hotel::getAddress).orElse(null))
                .roomImages(room.map(Room::getImages).orElse(List.of()))
                .hotelImages(hotel.map(Hotel::getImages).orElse(List.of()))
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .numNights(booking.getNumNights())
                .numGuests(booking.getNumGuests())
                .numRooms(booking.getNumRooms())
                .unitPrice(booking.getUnitPrice())
                .discountAmount(booking.getDiscountAmount())
                .voucherCode(booking.getVoucherCode())
                .totalPrice(booking.getTotalPrice())
                .status(booking.getStatus().name())
                .paymentStatus(booking.getPaymentStatus().name())
                .paymentMethod(booking.getPaymentMethod())
                .paidAt(booking.getPaidAt())
                .paymentExpiresAt(paymentDeadlinePolicy.expiresAt(booking))
                .refundAmount(booking.getRefundAmount())
                .cancellationReason(booking.getCancellationReason())
                .cancelledAt(booking.getCancelledAt())
                .cancelledBy(booking.getCancelledBy() != null ? booking.getCancelledBy().name() : null)
                .specialRequest(booking.getSpecialRequest())
                .guestName(booking.getGuestName())
                .guestEmail(booking.getGuestEmail())
                .guestPhone(booking.getGuestPhone())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}
