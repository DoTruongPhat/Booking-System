package com.booking.camunda.domain.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HotelCreatedEvent(
        String hotelId,
        String ownerUserId,
        String name,
        String city,
        String hostEmail
) {
}
