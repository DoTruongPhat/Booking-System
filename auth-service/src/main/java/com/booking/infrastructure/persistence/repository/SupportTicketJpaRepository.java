package com.booking.infrastructure.persistence.repository;

import com.booking.infrastructure.persistence.entity.SupportTicketEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SupportTicketJpaRepository
                            extends JpaRepository<SupportTicketEntity, UUID> {
    // User xem tickets của mình
    Page<SupportTicketEntity> findByCreatedBy(UUID createdBy, Pageable pageable);

    // Admin xem tickets assigned cho staff
    Page<SupportTicketEntity> findByAssignedTo(UUID assignedTo, Pageable pageable);

    // Admin xem theo status
    Page<SupportTicketEntity> findByStatus(String status, Pageable pageable);
}
