package com.booking.infrastructure.persistence.mapper;

import com.booking.domain.enums.RoomStatus;
import com.booking.domain.model.Room;
import com.booking.infrastructure.persistence.entity.HotelEntity;
import com.booking.infrastructure.persistence.entity.RoomEntity;
import org.springframework.stereotype.Component;

@Component
public class RoomMapper {

    public Room toDomain(RoomEntity entity) {
        if (entity == null) return null;

        Room room = new Room();
        room.setId(entity.getId());
        room.setHotelId(entity.getHotel() != null ? entity.getHotel().getId() : null);
        room.setRoomType(entity.getRoomType());
        room.setName(entity.getName());
        room.setDescription(entity.getDescription());
        room.setCapacity(entity.getCapacity());
        room.setBasePrice(entity.getBasePrice());
        room.setTotalRooms(entity.getTotalRooms());
        room.setAmenities(entity.getAmenities());
        room.setStatus(RoomStatus.valueOf(entity.getStatus()));
        room.setImages(entity.getImages());
        room.setCreatedAt(entity.getCreatedAt());
        room.setUpdatedAt(entity.getUpdatedAt());
        return room;
    }

    public RoomEntity toEntity(Room domain, HotelEntity hotelEntity) {
        if (domain == null) return null;

        return RoomEntity.builder()
                .id(domain.getId())
                .hotel(hotelEntity)
                .roomType(domain.getRoomType())
                .name(domain.getName())
                .description(domain.getDescription())
                .capacity(domain.getCapacity())
                .basePrice(domain.getBasePrice())
                .totalRooms(domain.getTotalRooms())
                .amenities(domain.getAmenities())
                .status(domain.getStatus().name())
                .images(domain.getImages())
                .build();
    }

    public void updateEntity(RoomEntity entity, Room domain) {
        entity.setRoomType(domain.getRoomType());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setCapacity(domain.getCapacity());
        entity.setBasePrice(domain.getBasePrice());
        entity.setTotalRooms(domain.getTotalRooms());
        entity.setAmenities(domain.getAmenities());
        entity.setStatus(domain.getStatus().name());
        entity.setImages(domain.getImages());
    }
}
