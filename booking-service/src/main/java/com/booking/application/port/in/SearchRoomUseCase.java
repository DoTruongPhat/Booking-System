package com.booking.application.port.in;

import com.booking.domain.model.Room;
import com.booking.domain.model.RoomAvailability;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface SearchRoomUseCase {

    Page<RoomSearchResult> searchRooms(SearchCriteria criteria, Pageable pageable);

    RoomDetailResult getRoomDetail(UUID roomId);

    record SearchCriteria(
            String city,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests,
            BigDecimal minPrice,    // nullable — optional filter
            BigDecimal maxPrice,    // nullable — optional filter
            BigDecimal minRating    // nullable — optional filter
    ) {}

    record RoomSearchResult(
            UUID roomId,
            UUID hotelId,
            String hotelName,
            String hotelCity,
            String roomName,
            String roomType,
            int capacity,
            int totalRooms,
            BigDecimal minPrice,
            BigDecimal basePrice,
            List<String> roomAmenities,
            List<String> hotelAmenities,
            List<String> roomImages,
            BigDecimal hotelRating
    ) {}

    record RoomDetailResult(
            Room room,
            String hotelName,
            String hotelCity,
            String hotelAddress,
            BigDecimal hotelRating,
            List<String> hotelAmenities,
            List<String> hotelImages,
            LocalTime checkInTime,
            LocalTime checkOutTime,
            List<RoomAvailability> availabilities
    ) {}
}