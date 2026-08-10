package com.booking.payment.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "processed_events", schema = "payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ProcessedEventEntity.ProcessedEventId.class)
public class ProcessedEventEntity {

    @Id
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Id
    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "processed_at", nullable = false)
    @Builder.Default
    private Instant processedAt = Instant.now();

    /**
     * Composite PK class.
     * Same event_id allowed across different event_types.
     */
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessedEventId implements Serializable {
        private String eventType;
        private String eventId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProcessedEventId that = (ProcessedEventId) o;
            return Objects.equals(eventType, that.eventType)
                    && Objects.equals(eventId, that.eventId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(eventType, eventId);
        }
    }
}