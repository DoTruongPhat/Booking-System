package com.booking.application.port.in;

import com.booking.domain.model.SupportTicket;

import java.util.UUID;

public interface ManageTicketUseCase {
    SupportTicket assignTicket(UUID ticketId, UUID staffId);
    SupportTicket updateTicketStatus(UUID ticketId, String status);
}
