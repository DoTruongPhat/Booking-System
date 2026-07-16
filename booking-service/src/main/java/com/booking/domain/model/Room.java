package com.booking.domain.model;

import com.booking.domain.enums.RoomStatus;
import com.booking.domain.enums.RoomType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Room {

    private UUID id;
    private UUID hotelId;
    private RoomType roomType;
    private String name;
    private String description;
    private Integer capacity;
    private BigDecimal basePrice;
    private Integer totalRooms;
    private List<String> amenities;
    private RoomStatus status;
    private List<String> images;
    private Instant createdAt;
    private Instant updatedAt;

    public Room() {
        this.amenities = new ArrayList<>();
        this.images = new ArrayList<>();
        this.status = RoomStatus.AVAILABLE;
    }

    // ─── Domain logic ────────────────────────

    public boolean isAvailable() {
        return this.status == RoomStatus.AVAILABLE;
    }

    public boolean canAccommodate(int guests, int numRooms) {
        return guests <= this.capacity * numRooms;
    }

    // ─── Getters / Setters ───────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getHotelId() { return hotelId; }
    public void setHotelId(UUID hotelId) { this.hotelId = hotelId; }

    public RoomType getRoomType() { return roomType; }
    public void setRoomType(RoomType roomType) { this.roomType = roomType; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public Integer getTotalRooms() { return totalRooms; }
    public void setTotalRooms(Integer totalRooms) { this.totalRooms = totalRooms; }

    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }

    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}