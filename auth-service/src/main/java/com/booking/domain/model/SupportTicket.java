package com.booking.domain.model;

import com.booking.domain.enums.TicketPriority;
import com.booking.domain.enums.TicketStatus;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * SupportTicket domain model - Pure Java
 *
 * Flow:
 * User tạo ticket (OPEN)
 *   → Admin assign cho Staff (IN_PROGRESS)
 *   → Staff xử lý xong (RESOLVED)
 *   → Admin/User close (CLOSED)
 */
public class SupportTicket {
    private UUID id;
    private String title;
    private String description;
    private TicketStatus status = TicketStatus.OPEN;
    private TicketPriority priority = TicketPriority.MEDIUM;
    private UUID createdBy;
    private UUID assignedTo;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private ZonedDateTime closedAt;

    public SupportTicket() {
    }

    // ── Business Methods ──────────────────────────────────────

    /**
     * Assign ticket cho staff
     */
    public void assign(UUID staffId) {
        this.assignedTo = staffId;
        this.status = TicketStatus.IN_PROGRESS;
        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * Staff resolve ticket
     */
    public void resolve() {
        this.status = TicketStatus.RESOLVED;
        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * Close ticket
     */
    public void close() {
        this.status = TicketStatus.CLOSED;
        this.closedAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * Kiểm tra ticket có thể close không
     */
    public boolean canClose() {
        return status != TicketStatus.CLOSED;
    }

    /**
     * Kiểm tra ticket có thể assign không
     */
    public boolean canAssign() {
        return status == TicketStatus.OPEN || status == TicketStatus.IN_PROGRESS;
    }

    // ── Getters/Setters ──────────────────────────────────────


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(UUID assignedTo) {
        this.assignedTo = assignedTo;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public ZonedDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(ZonedDateTime closedAt) {
        this.closedAt = closedAt;
    }
}
