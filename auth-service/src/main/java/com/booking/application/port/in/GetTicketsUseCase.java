package com.booking.application.port.in;

import com.booking.domain.model.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface GetTicketsUseCase {
    Page<SupportTicket> getMyTickets(UUID userId, Pageable pageable);
    SupportTicket getTicketById(UUID ticketId);
    Page<SupportTicket> getAllTickets(Pageable pageable);
    Page<SupportTicket> getTicketsByStatus(String status, Pageable pageable);
}
