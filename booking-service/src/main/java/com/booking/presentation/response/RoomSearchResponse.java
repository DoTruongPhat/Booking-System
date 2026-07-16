package com.booking.presentation.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class RoomSearchResponse {

    private UUID roomId;
    private UUID hotelId;
    private String hotelName;
    private String hotelCity;
    private String roomName;
    private String roomType;
    private int capacity;
    private int totalRooms;
    private BigDecimal minPrice;
    private BigDecimal basePrice;
    private List<String> roomAmenities;
    private List<String> hotelAmenities;
    private List<String> roomImages;
    private BigDecimal hotelRating;
}