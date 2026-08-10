package com.booking.presentation.mapper;

import com.booking.domain.enums.RoomStatus;
import com.booking.domain.model.Room;
import com.booking.infrastructure.persistence.repository.RoomAvailabilityJpaRepository;
import com.booking.presentation.request.CreateRoomRequest;
import com.booking.presentation.request.UpdateRoomRequest;
import com.booking.presentation.response.RoomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class RoomDtoMapper {

    private final RoomAvailabilityJpaRepository availabilityRepository;

    public Room toDomain(CreateRoomRequest request) {
        Room room = new Room();
        room.setRoomType(normalizeRoomType(request.getRoomType()));
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
        room.setRoomType(normalizeRoomType(request.getRoomType()));
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
                .roomType(room.getRoomType())
                .name(room.getName())
                .description(room.getDescription())
                .capacity(room.getCapacity())
                .basePrice(room.getBasePrice())
                .totalRooms(room.getTotalRooms())
                .available(resolveAvailable(room))
                .amenities(room.getAmenities())
                .status(room.getStatus().name())
                .images(room.getImages())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    private String normalizeRoomType(String value) {
        return value == null ? null : value.trim().toUpperCase().replaceAll("\\s+", "_");
    }

    private Integer resolveAvailable(Room room) {
        if (room.getId() == null) {
            return room.getTotalRooms();
        }

        LocalDate today = LocalDate.now();
        Integer minAvailable = availabilityRepository.findMinAvailableCount(
                room.getId(),
                today,
                today.plusDays(30)
        );

        return minAvailable != null ? minAvailable : room.getTotalRooms();
    }
}
