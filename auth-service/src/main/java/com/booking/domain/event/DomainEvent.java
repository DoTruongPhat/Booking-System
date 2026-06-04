package com.booking.domain.event;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * DomainEvent = base class cho tất cả domain events
 * → Pure Java, không phụ thuộc framework
 * → Mô tả điều gì đó ĐÃ XẢY RA trong domain (quá khứ)
 */
public abstract class DomainEvent {

    private final String eventId = UUID.randomUUID().toString();

    private final ZonedDateTime occurredAt = ZonedDateTime.now();

    public String getEventId() {
        return eventId;
    }

    public ZonedDateTime getOccurredAt() {
        return occurredAt;
    }
}
