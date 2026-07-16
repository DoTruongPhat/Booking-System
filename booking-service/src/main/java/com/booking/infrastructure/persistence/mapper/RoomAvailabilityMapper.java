package com.booking.infrastructure.persistence.mapper;

import com.booking.domain.enums.AvailabilityStatus;
import com.booking.domain.model.RoomAvailability;
import com.booking.infrastructure.persistence.entity.RoomAvailabilityEntity;
import com.booking.infrastructure.persistence.entity.RoomEntity;
import org.springframework.stereotype.Component;

@Component
public class RoomAvailabilityMapper {

    public RoomAvailability toDomain(RoomAvailabilityEntity entity) {
        if (entity == null) return null;

        RoomAvailability avail = new RoomAvailability();
        avail.setId(entity.getId());
        avail.setRoomId(entity.getRoom() != null ? entity.getRoom().getId() : null);
        avail.setDate(entity.getDate());
        avail.setAvailableCount(entity.getAvailableCount());
        avail.setPriceOverride(entity.getPriceOverride());
        avail.setStatus(AvailabilityStatus.valueOf(entity.getStatus()));
        avail.setCreatedAt(entity.getCreatedAt());
        avail.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getRoom() != null) {
            avail.setRoomBasePrice(entity.getRoom().getBasePrice());
        }

        return avail;
    }

    public RoomAvailabilityEntity toEntity(RoomAvailability domain, RoomEntity roomEntity) {
        if (domain == null) return null;

        return RoomAvailabilityEntity.builder()
                .id(domain.getId())
                .room(roomEntity)
                .date(domain.getDate())
                .availableCount(domain.getAvailableCount())
                .priceOverride(domain.getPriceOverride())
                .status(domain.getStatus().name())
                .build();
    }

    public void updateEntity(RoomAvailabilityEntity entity, RoomAvailability domain) {
        entity.setAvailableCount(domain.getAvailableCount());
        entity.setPriceOverride(domain.getPriceOverride());
        entity.setStatus(domain.getStatus().name());
    }
}