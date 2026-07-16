package com.booking.presentation.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class HotelResponse {

    private UUID id;
    private UUID ownerUserId;
    private String name;
    private String description;
    private String address;
    private String city;
    private String country;
    private BigDecimal rating;
    private String status;
    private List<String> amenities;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private List<String> images;
    private Instant createdAt;
    private Instant updatedAt;
}