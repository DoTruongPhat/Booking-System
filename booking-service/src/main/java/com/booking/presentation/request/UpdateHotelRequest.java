package com.booking.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class UpdateHotelRequest {

    @NotBlank(message = "Hotel name is required")
    @Size(max = 255)
    private String name;

    private String description;

    @NotBlank(message = "Address is required")
    @Size(max = 500)
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @NotBlank(message = "Country is required")
    @Size(max = 100)
    private String country;

    private List<String> amenities;

    private LocalTime checkInTime;

    private LocalTime checkOutTime;

    private List<String> images;
}