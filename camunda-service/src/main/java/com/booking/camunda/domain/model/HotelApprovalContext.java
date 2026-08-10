package com.booking.camunda.domain.model;

import java.util.Map;

public record HotelApprovalContext(
        String hotelId,
        String changeRequestId,
        String hostId,
        String hotelName,
        String city,
        String hostEmail,
        String workflowType,
        Map<String, Object> proposedChanges
) {
    public HotelApprovalContext(
            String hotelId,
            String hostId,
            String hotelName,
            String city,
            String hostEmail
    ) {
        this(hotelId, null, hostId, hotelName, city, hostEmail, "CREATE_HOTEL", Map.of());
    }

    public String businessKey() {
        return "UPDATE_HOTEL".equals(workflowType) && changeRequestId != null
                ? changeRequestId
                : hotelId;
    }
}
