package com.booking.application.service.serviceimpl;

import com.booking.application.port.in.CreateTicketUseCase;
import com.booking.application.port.in.GetTicketsUseCase;
import com.booking.application.port.in.ManageTicketUseCase;
import com.booking.application.port.out.SupportTicketRepositoryPort;
import com.booking.application.service.SupportTicketService;
import com.booking.domain.enums.TicketPriority;
import com.booking.domain.enums.TicketStatus;
import com.booking.domain.exception.ErrorCode;
import com.booking.domain.exception.ValidationException;
import com.booking.domain.model.SupportTicket;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class SupportTicketServiceImpl implements SupportTicketService,
        CreateTicketUseCase,
        GetTicketsUseCase,
        ManageTicketUseCase {

    private final SupportTicketRepositoryPort supportTicketRepositoryPort;

    @Override
    @Transactional
    public SupportTicket createTicket(UUID userId, String title,
                                      String description, String priority) {
        log.info("[Ticket] Creating ticket by user: {}", userId);
        SupportTicket ticket = new SupportTicket();
        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setCreatedBy(userId);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(priority != null
        ? TicketPriority.valueOf(priority.toUpperCase())
        : TicketPriority.MEDIUM);

        SupportTicket saved = supportTicketRepositoryPort.save(ticket);
        log.info("[Ticket] Created ticket : {}", saved.getId());
        return saved;

    }

    @Override
    public Page<SupportTicket> getMyTickets(UUID userId, Pageable pageable) {
        log.info("[Ticket] Get tickets for user: {}", userId);
        return supportTicketRepositoryPort.findByCreatedBy(userId, pageable);
    }

    @Override
    public SupportTicket getTicketById(UUID ticketId) {
        return supportTicketRepositoryPort.findById(ticketId)
                .orElseThrow(() -> new ValidationException(
                        ErrorCode.CMN_006, ErrorCode.CMN_006_MSG + ticketId
                ));
    }

    @Override
    public Page<SupportTicket> getAllTickets(Pageable pageable) {
        log.info("[Ticket] Get all tickets");
        return supportTicketRepositoryPort.findAll(pageable);
    }

    @Override
    public Page<SupportTicket> getTicketsByStatus(String status, Pageable pageable) {
        log.info("[Ticket] Get tickets by status: {}", status);
        return supportTicketRepositoryPort.findByStatus(status, pageable);
    }

    @Override
    @Transactional
    public SupportTicket assignTicket(UUID ticketId, UUID staffId) {
        log.info("[Ticket] Assign ticket {} to staff {}", ticketId, staffId);
        SupportTicket ticket = getTicketById(ticketId);

        if (!ticket.canAssign()){
            throw new ValidationException(
                    ErrorCode.CMN_007, ErrorCode.CMN_007_MSG + ticket.getStatus());
        }
        ticket.assign(staffId);
        return supportTicketRepositoryPort.save(ticket);
    }

    @Override
    @Transactional
    public SupportTicket updateTicketStatus(UUID ticketId, String status) {
        log.info("[Ticket] Update ticket {} status to {}", ticketId, status);

        SupportTicket ticket = getTicketById(ticketId);
        TicketStatus newStatus = TicketStatus.valueOf(status.toUpperCase());

        if (newStatus == TicketStatus.CLOSED) {
            if (!ticket.canClose()) {
                throw new ValidationException(ErrorCode.CMN_008,
                        ErrorCode.CMN_008_MSG);
            }
            ticket.close();
        } else if (newStatus == TicketStatus.RESOLVED) {
            ticket.resolve();
        } else {
            ticket.setStatus(newStatus);
        }

        return supportTicketRepositoryPort.save(ticket);
    }
}
