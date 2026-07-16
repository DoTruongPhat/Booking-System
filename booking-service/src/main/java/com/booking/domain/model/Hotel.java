package com.booking.domain.model;

import com.booking.domain.enums.HotelStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Hotel {

    private UUID id;
    private UUID ownerUserId;
    private String name;
    private String description;
    private String address;
    private String city;
    private String country;
    private BigDecimal rating;
    private HotelStatus status;
    private List<String> amenities;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private List<String> images;
    private Instant createdAt;
    private Instant updatedAt;

    public Hotel() {
        this.rating = BigDecimal.ZERO;
        this.status = HotelStatus.PENDING_APPROVAL;
        this.amenities = new ArrayList<>();
        this.images = new ArrayList<>();
        this.checkInTime = LocalTime.of(14, 0);
        this.checkOutTime = LocalTime.of(12, 0);
    }

    // ─── Domain logic ────────────────────────

    public boolean isOwnedBy(UUID userId) {
        return this.ownerUserId.equals(userId);
    }

    public boolean isActive() {
        return this.status == HotelStatus.ACTIVE;
    }

    public void approve() {
        this.status = HotelStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = HotelStatus.INACTIVE;
        this.updatedAt = Instant.now();
    }

    // ─── Getters / Setters ───────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(UUID ownerUserId) { this.ownerUserId = ownerUserId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }

    public HotelStatus getStatus() { return status; }
    public void setStatus(HotelStatus status) { this.status = status; }

    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }

    public LocalTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalTime checkInTime) { this.checkInTime = checkInTime; }

    public LocalTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalTime checkOutTime) { this.checkOutTime = checkOutTime; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}