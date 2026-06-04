package com.booking.application.port.out;

import com.booking.domain.model.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface SupportTicketRepositoryPort {
    SupportTicket save(SupportTicket ticket);
    Optional<SupportTicket> findById(UUID id);
    Page<SupportTicket> findByCreatedBy(UUID createdBy, Pageable pageable);
    Page<SupportTicket> findByAssignedTo(UUID assignedTo, Pageable pageable);
    Page<SupportTicket> findByStatus(String status, Pageable pageable);
    Page<SupportTicket> findAll(Pageable pageable);
}
