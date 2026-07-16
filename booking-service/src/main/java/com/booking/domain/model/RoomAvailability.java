package com.booking.domain.model;

import com.booking.domain.enums.AvailabilityStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class RoomAvailability {

    private UUID id;
    private UUID roomId;
    private LocalDate date;
    private Integer availableCount;
    private BigDecimal priceOverride;
    private AvailabilityStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    // Transient — loaded from Room when needed for price calculation
    private BigDecimal roomBasePrice;

    public RoomAvailability() {
        this.status = AvailabilityStatus.AVAILABLE;
    }

    // ─── Domain logic ────────────────────────

    public boolean isAvailable() {
        return this.status == AvailabilityStatus.AVAILABLE && this.availableCount > 0;
    }

    public boolean hasEnoughRooms(int requested) {
        return this.status == AvailabilityStatus.AVAILABLE && this.availableCount >= requested;
    }

    public BigDecimal getEffectivePrice() {
        if (priceOverride != null) {
            return priceOverride;
        }
        return roomBasePrice != null ? roomBasePrice : BigDecimal.ZERO;
    }

    public void decrementCount(int numRooms) {
        this.availableCount -= numRooms;
    }

    public void incrementCount(int numRooms) {
        this.availableCount += numRooms;
    }

    public void block() {
        this.status = AvailabilityStatus.BLOCKED;
        this.availableCount = 0;
    }

    public void unblock(int totalRooms) {
        this.status = AvailabilityStatus.AVAILABLE;
        this.availableCount = totalRooms;
    }

    // ─── Getters / Setters ───────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getRoomId() { return roomId; }
    public void setRoomId(UUID roomId) { this.roomId = roomId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Integer getAvailableCount() { return availableCount; }
    public void setAvailableCount(Integer availableCount) { this.availableCount = availableCount; }

    public BigDecimal getPriceOverride() { return priceOverride; }
    public void setPriceOverride(BigDecimal priceOverride) { this.priceOverride = priceOverride; }

    public AvailabilityStatus getStatus() { return status; }
    public void setStatus(AvailabilityStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public BigDecimal getRoomBasePrice() { return roomBasePrice; }
    public void setRoomBasePrice(BigDecimal roomBasePrice) { this.roomBasePrice = roomBasePrice; }
}