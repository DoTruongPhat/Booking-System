package com.booking.presentation.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class RoomResponse {

    private UUID id;
    private UUID hotelId;
    private String roomType;
    private String name;
    private String description;
    private Integer capacity;
    private BigDecimal basePrice;
    private Integer totalRooms;
    private List<String> amenities;
    private String status;
    private List<String> images;
    private Instant createdAt;
    private Instant updatedAt;
}