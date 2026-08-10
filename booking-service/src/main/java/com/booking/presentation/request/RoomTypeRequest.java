package com.booking.presentation.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class RoomTypeRequest {

    private UUID hotelId;

    @NotBlank(message = "Room type code is required")
    private String code;

    @NotBlank(message = "Room type name is required")
    private String name;

    private String description;

    @Min(value = 1, message = "Default capacity must be at least 1")
    private Integer defaultCapacity;

    private List<String> defaultAmenities;

    private Boolean active;
}
