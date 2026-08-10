package com.booking.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "room_types", schema = "booking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    private HotelEntity hotel;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    private String description;

    @Column(name = "default_capacity")
    private Integer defaultCapacity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "default_amenities", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> defaultAmenities = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        code = normalizeCode(code);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        code = normalizeCode(code);
    }

    private String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase().replaceAll("\\s+", "_");
    }
}
