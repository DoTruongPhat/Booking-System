package com.booking.application.service;

import com.booking.domain.model.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SupportTicketService {
    // User
    SupportTicket createTicket(UUID userId, String title, String description, String priority);
    Page<SupportTicket> getMyTickets(UUID userId, Pageable pageable);
    SupportTicket getTicketById(UUID ticketId);

    // Admin
    Page<SupportTicket> getAllTickets(Pageable pageable);
    Page<SupportTicket> getTicketsByStatus(String status, Pageable pageable);
    SupportTicket assignTicket(UUID ticketId, UUID staffId);
    SupportTicket updateTicketStatus(UUID ticketId, String status);
}
