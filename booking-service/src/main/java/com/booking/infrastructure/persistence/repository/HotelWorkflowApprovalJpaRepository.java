package com.booking.infrastructure.persistence.repository;

import com.booking.infrastructure.persistence.entity.HotelWorkflowApprovalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HotelWorkflowApprovalJpaRepository extends JpaRepository<HotelWorkflowApprovalEntity, UUID> {

    Optional<HotelWorkflowApprovalEntity> findByBusinessKey(String businessKey);

    Optional<HotelWorkflowApprovalEntity> findByCurrentTaskId(String currentTaskId);

    Optional<HotelWorkflowApprovalEntity> findTopByHotelIdAndWorkflowTypeOrderByCreatedAtDesc(
            UUID hotelId,
            String workflowType
    );
}
