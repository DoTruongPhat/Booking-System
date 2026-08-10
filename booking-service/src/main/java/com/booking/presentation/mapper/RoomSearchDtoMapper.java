package com.booking.presentation.mapper;

import com.booking.application.port.in.SearchRoomUseCase.RoomDetailResult;
import com.booking.application.port.in.SearchRoomUseCase.RoomSearchResult;
import com.booking.domain.model.Room;
import com.booking.domain.model.RoomAvailability;
import com.booking.presentation.response.RoomDetailResponse;
import com.booking.presentation.response.RoomSearchResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoomSearchDtoMapper {

    public RoomSearchResponse toSearchResponse(RoomSearchResult result) {
        return RoomSearchResponse.builder()
                .roomId(result.roomId())
                .hotelId(result.hotelId())
                .hotelName(result.hotelName())
                .hotelCity(result.hotelCity())
                .roomName(result.roomName())
                .roomType(result.roomType())
                .capacity(result.capacity())
                .totalRooms(result.totalRooms())
                .minPrice(result.minPrice())
                .basePrice(result.basePrice())
                .roomAmenities(result.roomAmenities())
                .hotelAmenities(result.hotelAmenities())
                .roomImages(result.roomImages())
                .hotelRating(result.hotelRating())
                .build();
    }

    public RoomDetailResponse toDetailResponse(RoomDetailResult result) {
        Room room = result.room();

        List<RoomDetailResponse.AvailabilitySlot> slots = result.availabilities().stream()
                .map(this::toSlot)
                .toList();

        return RoomDetailResponse.builder()
                .roomId(room.getId())
                .roomName(room.getName())
                .roomType(room.getRoomType())
                .roomDescription(room.getDescription())
                .capacity(room.getCapacity())
                .basePrice(room.getBasePrice())
                .totalRooms(room.getTotalRooms())
                .roomAmenities(room.getAmenities())
                .roomStatus(room.getStatus().name())
                .roomImages(room.getImages())
                .hotelId(room.getHotelId())
                .hotelName(result.hotelName())
                .hotelCity(result.hotelCity())
                .hotelAddress(result.hotelAddress())
                .hotelRating(result.hotelRating())
                .hotelAmenities(result.hotelAmenities())
                .hotelImages(result.hotelImages())
                .checkInTime(result.checkInTime())
                .checkOutTime(result.checkOutTime())
                .availabilities(slots)
                .build();
    }

    private RoomDetailResponse.AvailabilitySlot toSlot(RoomAvailability avail) {
        return RoomDetailResponse.AvailabilitySlot.builder()
                .date(avail.getDate())
                .availableCount(avail.getAvailableCount())
                .effectivePrice(avail.getEffectivePrice())
                .status(avail.getStatus().name())
                .build();
    }
}
