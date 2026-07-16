package com.booking.domain.model;

import com.booking.domain.enums.BookingStatus;
import com.booking.domain.enums.CancelledBy;
import com.booking.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class Booking {

    private UUID id;
    private String bookingCode;
    private UUID userId;
    private UUID roomId;
    private UUID hotelId;

    // Dates
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer numNights;

    // Guests & Rooms
    private Integer numGuests;
    private Integer numRooms;

    // Price
    private BigDecimal unitPrice;
    private BigDecimal discountAmount;
    private BigDecimal totalPrice;

    // Status
    private BookingStatus status;

    // Payment
    private PaymentStatus paymentStatus;
    private String paymentMethod;
    private Instant paidAt;
    private BigDecimal refundAmount;

    // Cancellation
    private String cancellationReason;
    private Instant cancelledAt;
    private CancelledBy cancelledBy;

    // Extra
    private String specialRequest;

    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;

    private String guestName;
    private String guestEmail;
    private String guestPhone;

    public Booking() {
        this.numRooms = 1;
        this.discountAmount = BigDecimal.ZERO;
        this.status = BookingStatus.PENDING;
        this.paymentStatus = PaymentStatus.UNPAID;
    }

    // ─── Domain logic ────────────────────────

    public boolean isOwnedBy(UUID userId) {
        return this.userId.equals(userId);
    }

    public boolean isCancellable() {
        return this.status == BookingStatus.PENDING || this.status == BookingStatus.CONFIRMED;
    }

    public void cancel(CancelledBy by, String reason) {
        this.status = BookingStatus.CANCELLED;
        this.cancelledBy = by;
        this.cancellationReason = reason;
        this.cancelledAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void confirm() {
        this.status = BookingStatus.CONFIRMED;
        this.updatedAt = Instant.now();
    }

    public void markNoShow() {
        this.status = BookingStatus.NO_SHOW;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        this.status = BookingStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }

    // ─── Getters / Setters ───────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getRoomId() { return roomId; }
    public void setRoomId(UUID roomId) { this.roomId = roomId; }

    public UUID getHotelId() { return hotelId; }
    public void setHotelId(UUID hotelId) { this.hotelId = hotelId; }

    public LocalDate getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }

    public LocalDate getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; }

    public Integer getNumNights() { return numNights; }
    public void setNumNights(Integer numNights) { this.numNights = numNights; }

    public Integer getNumGuests() { return numGuests; }
    public void setNumGuests(Integer numGuests) { this.numGuests = numGuests; }

    public Integer getNumRooms() { return numRooms; }
    public void setNumRooms(Integer numRooms) { this.numRooms = numRooms; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }

    public CancelledBy getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(CancelledBy cancelledBy) { this.cancelledBy = cancelledBy; }

    public String getSpecialRequest() { return specialRequest; }
    public void setSpecialRequest(String specialRequest) { this.specialRequest = specialRequest; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }

    public String getGuestEmail() { return guestEmail; }
    public void setGuestEmail(String guestEmail) { this.guestEmail = guestEmail; }

    public String getGuestPhone() { return guestPhone; }
    public void setGuestPhone(String guestPhone) { this.guestPhone = guestPhone; }
}