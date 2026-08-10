package com.booking.payment.domain.model;

import java.time.Instant;

/**
 * Composite key: (eventType, eventId)
 * Avoid conflict when different event types use same ID.
 */
public class ProcessedEvent {

    private String eventId;
    private String eventType;
    private Instant processedAt;

    public ProcessedEvent() {
        this.processedAt = Instant.now();
    }

    public ProcessedEvent(String eventType, String eventId) {
        this.eventType = eventType;
        this.eventId = eventId;
        this.processedAt = Instant.now();
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}