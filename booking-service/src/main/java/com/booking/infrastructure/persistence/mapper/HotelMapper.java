package com.booking.infrastructure.persistence.mapper;

import com.booking.domain.enums.HotelStatus;
import com.booking.domain.model.Hotel;
import com.booking.infrastructure.persistence.entity.HotelEntity;
import org.springframework.stereotype.Component;

@Component
public class HotelMapper {

    public Hotel toDomain(HotelEntity entity) {
        if (entity == null) return null;

        Hotel hotel = new Hotel();
        hotel.setId(entity.getId());
        hotel.setOwnerUserId(entity.getOwnerUserId());
        hotel.setName(entity.getName());
        hotel.setDescription(entity.getDescription());
        hotel.setAddress(entity.getAddress());
        hotel.setCity(entity.getCity());
        hotel.setCountry(entity.getCountry());
        hotel.setRating(entity.getRating());
        hotel.setStatus(HotelStatus.valueOf(entity.getStatus()));
        hotel.setAmenities(entity.getAmenities());
        hotel.setCheckInTime(entity.getCheckInTime());
        hotel.setCheckOutTime(entity.getCheckOutTime());
        hotel.setImages(entity.getImages());
        hotel.setCreatedAt(entity.getCreatedAt());
        hotel.setUpdatedAt(entity.getUpdatedAt());
        return hotel;
    }

    public HotelEntity toEntity(Hotel domain) {
        if (domain == null) return null;

        return HotelEntity.builder()
                .id(domain.getId())
                .ownerUserId(domain.getOwnerUserId())
                .name(domain.getName())
                .description(domain.getDescription())
                .address(domain.getAddress())
                .city(domain.getCity())
                .country(domain.getCountry())
                .rating(domain.getRating())
                .status(domain.getStatus().name())
                .amenities(domain.getAmenities())
                .checkInTime(domain.getCheckInTime())
                .checkOutTime(domain.getCheckOutTime())
                .images(domain.getImages())
                .build();
    }

    public void updateEntity(HotelEntity entity, Hotel domain) {
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setAddress(domain.getAddress());
        entity.setCity(domain.getCity());
        entity.setCountry(domain.getCountry());
        entity.setStatus(domain.getStatus().name());
        entity.setAmenities(domain.getAmenities());
        entity.setCheckInTime(domain.getCheckInTime());
        entity.setCheckOutTime(domain.getCheckOutTime());
        entity.setImages(domain.getImages());
    }
}