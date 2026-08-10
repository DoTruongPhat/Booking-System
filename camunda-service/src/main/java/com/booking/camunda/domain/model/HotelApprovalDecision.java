package com.booking.camunda.domain.model;

public record HotelApprovalDecision(
        String taskId,
        String decision,
        String comment,
        String reviewerId
) {
    public String normalizedDecision() {
        return decision == null ? "" : decision.trim().toUpperCase();
    }
}
