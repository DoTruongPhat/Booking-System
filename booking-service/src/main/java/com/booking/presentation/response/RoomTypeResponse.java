package com.booking.presentation.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class RoomTypeResponse {
    private UUID id;
    private UUID hotelId;
    private String hotelName;
    private String code;
    private String name;
    private String description;
    private Integer defaultCapacity;
    private List<String> defaultAmenities;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
