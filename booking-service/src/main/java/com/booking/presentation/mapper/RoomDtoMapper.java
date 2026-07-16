package com.booking.presentation.mapper;

import com.booking.domain.enums.RoomStatus;
import com.booking.domain.enums.RoomType;
import com.booking.domain.model.Room;
import com.booking.presentation.request.CreateRoomRequest;
import com.booking.presentation.request.UpdateRoomRequest;
import com.booking.presentation.response.RoomResponse;
import org.springframework.stereotype.Component;

@Component
public class RoomDtoMapper {

    public Room toDomain(CreateRoomRequest request) {
        Room room = new Room();
        room.setRoomType(RoomType.valueOf(request.getRoomType()));
        room.setName(request.getName());
        room.setDescription(request.getDescription());
        room.setCapacity(request.getCapacity());
        room.setBasePrice(request.getBasePrice());
        room.setTotalRooms(request.getTotalRooms());

        if (request.getAmenities() != null) room.setAmenities(request.getAmenities());
        if (request.getImages() != null) room.setImages(request.getImages());

        return room;
    }

    public Room toDomain(UpdateRoomRequest request) {
        Room room = new Room();
        room.setRoomType(RoomType.valueOf(request.getRoomType()));
        room.setName(request.getName());
        room.setDescription(request.getDescription());
        room.setCapacity(request.getCapacity());
        room.setBasePrice(request.getBasePrice());
        room.setTotalRooms(request.getTotalRooms());

        if (request.getAmenities() != null) room.setAmenities(request.getAmenities());
        if (request.getImages() != null) room.setImages(request.getImages());
        if (request.getStatus() != null) room.setStatus(RoomStatus.valueOf(request.getStatus()));

        return room;
    }

    public RoomResponse toResponse(Room room) {
        return RoomResponse.builder()
                .id(room.getId())
                .hotelId(room.getHotelId())
                .roomType(room.getRoomType().name())
                .name(room.getName())
                .description(room.getDescription())
                .capacity(room.getCapacity())
                .basePrice(room.getBasePrice())
                .totalRooms(room.getTotalRooms())
                .amenities(room.getAmenities())
                .status(room.getStatus().name())
                .images(room.getImages())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }
}