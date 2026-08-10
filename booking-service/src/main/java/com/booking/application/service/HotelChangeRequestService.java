package com.booking.application.service;

import com.booking.domain.enums.HotelChangeRequestStatus;
import com.booking.domain.enums.HotelStatus;
import com.booking.domain.enums.HotelWorkflowType;
import com.booking.domain.event.CoreDomainEvent;
import com.booking.domain.exception.CoreErrorCode;
import com.booking.domain.exception.CoreException;
import com.booking.domain.model.Hotel;
import com.booking.infrastructure.persistence.entity.HotelChangeRequestEntity;
import com.booking.infrastructure.persistence.repository.HotelChangeRequestJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HotelChangeRequestService {

    private final HotelChangeRequestJpaRepository changeRequestRepository;
    private final HotelWorkflowSyncService workflowSyncService;

    @Transactional
    public HotelChangeRequestEntity createUpdateRequest(Hotel current, Hotel proposed, UUID ownerUserId) {
        changeRequestRepository
                .findFirstByHotelIdAndStatusOrderByCreatedAtDesc(
                        current.getId(),
                        HotelChangeRequestStatus.PENDING_APPROVAL.name())
                .ifPresent(existing -> {
                    throw new CoreException(
                            CoreErrorCode.INVALID_REQUEST,
                            "A hotel update request is already waiting for approval"
                    );
                });

        HotelChangeRequestEntity request = HotelChangeRequestEntity.builder()
                .hotelId(current.getId())
                .ownerUserId(ownerUserId)
                .proposedChanges(toChangeSnapshot(proposed))
                .status(HotelChangeRequestStatus.PENDING_APPROVAL.name())
                .build();

        HotelChangeRequestEntity saved = changeRequestRepository.save(request);
        workflowSyncService.createStartRequested(
                current.getId(),
                saved.getId(),
                HotelWorkflowType.UPDATE_HOTEL,
                saved.getId().toString(),
                current.getStatus()
        );
        return saved;
    }

    @Transactional
    public HotelChangeRequestEntity approve(UUID changeRequestId, String reviewerId, String comment) {
        HotelChangeRequestEntity request = findPending(changeRequestId);
        request.setStatus(HotelChangeRequestStatus.APPROVED.name());
        request.setReviewerId(reviewerId);
        request.setDecisionComment(comment);
        request.setCompletedAt(Instant.now());
        return request;
    }

    @Transactional
    public HotelChangeRequestEntity reject(UUID changeRequestId, String reviewerId, String reason) {
        HotelChangeRequestEntity request = findPending(changeRequestId);
        request.setStatus(HotelChangeRequestStatus.REJECTED.name());
        request.setReviewerId(reviewerId);
        request.setDecisionComment(reason);
        request.setCompletedAt(Instant.now());
        return request;
    }

    @Transactional(readOnly = true)
    public HotelChangeRequestEntity getById(UUID changeRequestId) {
        return changeRequestRepository.findById(changeRequestId)
                .orElseThrow(() -> new CoreException(CoreErrorCode.INVALID_REQUEST, "Hotel change request not found"));
    }

    public CoreDomainEvent.HotelChangeRequested toEvent(Hotel hotel, HotelChangeRequestEntity request, String hostEmail) {
        return new CoreDomainEvent.HotelChangeRequested(
                request.getId(),
                hotel.getId(),
                hotel.getOwnerUserId(),
                hotel.getName(),
                hotel.getCity(),
                hostEmail,
                request.getProposedChanges(),
                Instant.now()
        );
    }

    public Map<String, Object> toChangeSnapshot(Hotel proposed) {
        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("name", proposed.getName());
        changes.put("description", proposed.getDescription());
        changes.put("address", proposed.getAddress());
        changes.put("city", proposed.getCity());
        changes.put("country", proposed.getCountry());
        changes.put("amenities", proposed.getAmenities());
        changes.put("checkInTime", proposed.getCheckInTime() != null ? proposed.getCheckInTime().toString() : null);
        changes.put("checkOutTime", proposed.getCheckOutTime() != null ? proposed.getCheckOutTime().toString() : null);
        changes.put("images", proposed.getImages());
        return changes;
    }

    private HotelChangeRequestEntity findPending(UUID changeRequestId) {
        HotelChangeRequestEntity request = getById(changeRequestId);
        if (!HotelChangeRequestStatus.PENDING_APPROVAL.name().equals(request.getStatus())) {
            throw new CoreException(CoreErrorCode.INVALID_REQUEST, "Hotel change request is not pending");
        }
        return request;
    }
}
