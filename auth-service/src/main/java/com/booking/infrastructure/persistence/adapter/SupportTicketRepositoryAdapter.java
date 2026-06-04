package com.booking.infrastructure.persistence.adapter;

import com.booking.application.port.out.SupportTicketRepositoryPort;
import com.booking.domain.model.SupportTicket;
import com.booking.infrastructure.persistence.mapper.SupportTicketEntityMapper;
import com.booking.infrastructure.persistence.repository.SupportTicketJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SupportTicketRepositoryAdapter implements SupportTicketRepositoryPort {

    private final SupportTicketJpaRepository jpaRepository;
    private final SupportTicketEntityMapper mapper;

    @Override
    public SupportTicket save(SupportTicket ticket) {
        if (ticket.getId() != null) {
            // UPDATE — load entity cũ rồi update fields
            return jpaRepository.findById(ticket.getId())
                    .map(existing -> {
                        existing.setTitle(ticket.getTitle());
                        existing.setDescription(ticket.getDescription());
                        existing.setStatus(ticket.getStatus().name());
                        existing.setPriority(ticket.getPriority().name());
                        existing.setAssignedTo(ticket.getAssignedTo());
                        existing.setClosedAt(ticket.getClosedAt());
                        return mapper.toDomain(jpaRepository.save(existing));
                    })
                    .orElseGet(() -> mapper.toDomain(
                            jpaRepository.save(mapper.toEntity(ticket))));
        }

        return mapper.toDomain(
                jpaRepository.save(mapper.toEntity(ticket)));
    }

    @Override
    public Optional<SupportTicket> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Page<SupportTicket> findByCreatedBy(UUID createdBy, Pageable pageable) {
        return jpaRepository.findByCreatedBy(createdBy, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<SupportTicket> findByAssignedTo(UUID assignedTo, Pageable pageable) {
        return jpaRepository.findByAssignedTo(assignedTo, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<SupportTicket> findByStatus(String status, Pageable pageable) {
        return jpaRepository.findByStatus(status, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<SupportTicket> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable)
                .map(mapper::toDomain);
    }
}
