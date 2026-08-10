package com.booking.infrastructure.persistence.mapper;

import com.booking.domain.enums.BookingStatus;
import com.booking.domain.enums.CancelledBy;
import com.booking.domain.enums.PaymentStatus;
import com.booking.domain.model.Booking;
import com.booking.infrastructure.persistence.entity.BookingEntity;
import com.booking.infrastructure.persistence.entity.HotelEntity;
import com.booking.infrastructure.persistence.entity.RoomEntity;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public Booking toDomain(BookingEntity entity) {
        if (entity == null) return null;

        Booking booking = new Booking();
        booking.setId(entity.getId());
        booking.setBookingCode(entity.getBookingCode());
        booking.setUserId(entity.getUserId());
        booking.setRoomId(entity.getRoom() != null ? entity.getRoom().getId() : null);
        booking.setHotelId(entity.getHotel() != null ? entity.getHotel().getId() : null);
        booking.setCheckInDate(entity.getCheckInDate());
        booking.setCheckOutDate(entity.getCheckOutDate());
        booking.setNumNights(entity.getNumNights());
        booking.setNumGuests(entity.getNumGuests());
        booking.setNumRooms(entity.getNumRooms());
        booking.setUnitPrice(entity.getUnitPrice());
        booking.setDiscountAmount(entity.getDiscountAmount());
        booking.setVoucherCode(entity.getVoucherCode());
        booking.setTotalPrice(entity.getTotalPrice());
        booking.setStatus(BookingStatus.valueOf(entity.getStatus()));
        booking.setPaymentStatus(PaymentStatus.valueOf(entity.getPaymentStatus()));
        booking.setPaymentMethod(entity.getPaymentMethod());
        booking.setPaidAt(entity.getPaidAt());
        booking.setRefundAmount(entity.getRefundAmount());
        booking.setCancellationReason(entity.getCancellationReason());
        booking.setCancelledAt(entity.getCancelledAt());
        if (entity.getCancelledBy() != null) {
            booking.setCancelledBy(CancelledBy.valueOf(entity.getCancelledBy()));
        }
        booking.setSpecialRequest(entity.getSpecialRequest());
        booking.setCreatedAt(entity.getCreatedAt());
        booking.setUpdatedAt(entity.getUpdatedAt());
        booking.setGuestName(entity.getGuestName());
        booking.setGuestEmail(entity.getGuestEmail());
        booking.setGuestPhone(entity.getGuestPhone());
        return booking;
    }

    public BookingEntity toEntity(Booking domain, RoomEntity roomEntity, HotelEntity hotelEntity) {
        if (domain == null) return null;

        return BookingEntity.builder()
                .id(domain.getId())
                .bookingCode(domain.getBookingCode())
                .userId(domain.getUserId())
                .room(roomEntity)
                .hotel(hotelEntity)
                .checkInDate(domain.getCheckInDate())
                .checkOutDate(domain.getCheckOutDate())
                .numNights(domain.getNumNights())
                .numGuests(domain.getNumGuests())
                .numRooms(domain.getNumRooms())
                .unitPrice(domain.getUnitPrice())
                .discountAmount(domain.getDiscountAmount())
                .voucherCode(domain.getVoucherCode())
                .totalPrice(domain.getTotalPrice())
                .status(domain.getStatus().name())
                .paymentStatus(domain.getPaymentStatus().name())
                .paymentMethod(domain.getPaymentMethod())
                .paidAt(domain.getPaidAt())
                .refundAmount(domain.getRefundAmount())
                .cancellationReason(domain.getCancellationReason())
                .cancelledAt(domain.getCancelledAt())
                .cancelledBy(domain.getCancelledBy() != null ? domain.getCancelledBy().name() : null)
                .specialRequest(domain.getSpecialRequest())
                .guestName(domain.getGuestName())
                .guestEmail(domain.getGuestEmail())
                .guestPhone(domain.getGuestPhone())
                .build();
    }

    public void updateEntity(BookingEntity entity, Booking domain) {
        entity.setStatus(domain.getStatus().name());
        entity.setPaymentStatus(domain.getPaymentStatus().name());
        entity.setPaymentMethod(domain.getPaymentMethod());
        entity.setPaidAt(domain.getPaidAt());
        entity.setUnitPrice(domain.getUnitPrice());
        entity.setDiscountAmount(domain.getDiscountAmount());
        entity.setVoucherCode(domain.getVoucherCode());
        entity.setTotalPrice(domain.getTotalPrice());
        entity.setRefundAmount(domain.getRefundAmount());
        entity.setCancellationReason(domain.getCancellationReason());
        entity.setCancelledAt(domain.getCancelledAt());
        entity.setCancelledBy(domain.getCancelledBy() != null ? domain.getCancelledBy().name() : null);
        entity.setGuestName(domain.getGuestName());
        entity.setGuestEmail(domain.getGuestEmail());
        entity.setGuestPhone(domain.getGuestPhone());
    }
}
