package com.booking.domain.enums;

public enum HotelWorkflowStatus {
    START_REQUESTED,
    PROCESS_STARTED,
    WAITING_ADMIN_REVIEW,
    CLAIMED,
    APPROVING,
    REJECTING,
    APPROVED,
    REJECTED,
    INCIDENT,
    OUT_OF_SYNC
}
