package com.booking.domain.validation;

import com.booking.application.port.out.BookingRepositoryPort;
import com.booking.domain.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoomValidator {

    private final BookingRepositoryPort bookingRepository;

    /**
     * BR-ROOM-012: totalRooms không được giảm nhỏ hơn số booking đang active.
     */
    public void validateTotalRoomsReduction(UUID roomId, int newTotalRooms) {
        long activeBookings = bookingRepository.countActiveBookingsForRoom(roomId, LocalDate.now());

        if (newTotalRooms < activeBookings) {
            log.warn("[BR-ROOM-012] Cannot reduce totalRooms to {} for room {}. Active: {}",
                    newTotalRooms, roomId, activeBookings);
            throw new BusinessRuleException(
                    "BR-ROOM-012",
                    "Không thể giảm số phòng xuống " + newTotalRooms
                            + ". Hiện có " + activeBookings + " booking đang hoạt động."
            );
        }
    }
}
