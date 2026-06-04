package com.booking.domain.enums;

/**
 * Trạng thái Support Ticket
 *
 * OPEN → User vừa tạo, chưa ai xử lý
 * IN_PROGRESS → Admin đã assign cho Staff
 * RESOLVED → Staff đã xử lý xong
 * CLOSED → Admin/User đóng ticket
 */
public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED
}
