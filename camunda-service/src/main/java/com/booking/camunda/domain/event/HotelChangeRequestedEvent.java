package com.booking.camunda.domain.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HotelChangeRequestedEvent(
        String changeRequestId,
        String hotelId,
        String ownerUserId,
        String name,
        String city,
        String hostEmail,
        Map<String, Object> proposedChanges
) {
}
