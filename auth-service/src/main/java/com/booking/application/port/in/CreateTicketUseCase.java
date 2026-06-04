package com.booking.application.port.in;

import com.booking.domain.model.SupportTicket;

import java.util.UUID;

public interface CreateTicketUseCase {
    SupportTicket createTicket(UUID userId, String title,
                               String description, String priority);
}
