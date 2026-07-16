package com.booking.presentation.mapper;

import com.booking.domain.model.Hotel;
import com.booking.presentation.request.CreateHotelRequest;
import com.booking.presentation.request.UpdateHotelRequest;
import com.booking.presentation.response.HotelResponse;
import org.springframework.stereotype.Component;
/*
Request → Domain → Response
 */
@Component
public class HotelDtoMapper {

    public Hotel toDomain(CreateHotelRequest request) {
        Hotel hotel = new Hotel();
        hotel.setName(request.getName());
        hotel.setDescription(request.getDescription());
        hotel.setAddress(request.getAddress());
        hotel.setCity(request.getCity());
        hotel.setCountry(request.getCountry());

        if (request.getAmenities() != null) hotel.setAmenities(request.getAmenities());
        if (request.getCheckInTime() != null) hotel.setCheckInTime(request.getCheckInTime());
        if (request.getCheckOutTime() != null) hotel.setCheckOutTime(request.getCheckOutTime());
        if (request.getImages() != null) hotel.setImages(request.getImages());

        return hotel;
    }

    public Hotel toDomain(UpdateHotelRequest request) {
        Hotel hotel = new Hotel();
        hotel.setName(request.getName());
        hotel.setDescription(request.getDescription());
        hotel.setAddress(request.getAddress());
        hotel.setCity(request.getCity());
        hotel.setCountry(request.getCountry());

        if (request.getAmenities() != null) hotel.setAmenities(request.getAmenities());
        if (request.getCheckInTime() != null) hotel.setCheckInTime(request.getCheckInTime());
        if (request.getCheckOutTime() != null) hotel.setCheckOutTime(request.getCheckOutTime());
        if (request.getImages() != null) hotel.setImages(request.getImages());

        return hotel;
    }

    public HotelResponse toResponse(Hotel hotel) {
        return HotelResponse.builder()
                .id(hotel.getId())
                .ownerUserId(hotel.getOwnerUserId())
                .name(hotel.getName())
                .description(hotel.getDescription())
                .address(hotel.getAddress())
                .city(hotel.getCity())
                .country(hotel.getCountry())
                .rating(hotel.getRating())
                .status(hotel.getStatus().name())
                .amenities(hotel.getAmenities())
                .checkInTime(hotel.getCheckInTime())
                .checkOutTime(hotel.getCheckOutTime())
                .images(hotel.getImages())
                .createdAt(hotel.getCreatedAt())
                .updatedAt(hotel.getUpdatedAt())
                .build();
    }
}