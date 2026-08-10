package com.booking.application.service;

import com.booking.application.port.in.ApproveHotelUseCase;
import com.booking.application.port.in.CreateHotelUseCase;
import com.booking.application.port.in.DeactivateHotelUseCase;
import com.booking.application.port.in.DeleteHotelUseCase;
import com.booking.application.port.in.QueryHotelUseCase;
import com.booking.application.port.in.UpdateHotelUseCase;
import com.booking.application.port.out.AuditEventPort;
import com.booking.application.port.out.HotelEventPublisherPort;
import com.booking.application.port.out.HotelRepositoryPort;
import com.booking.domain.enums.HotelChangeRequestStatus;
import com.booking.domain.enums.HotelStatus;
import com.booking.domain.enums.HotelWorkflowType;
import com.booking.domain.event.CoreDomainEvent;
import com.booking.domain.exception.CoreErrorCode;
import com.booking.domain.exception.CoreException;
import com.booking.domain.model.Hotel;
import com.booking.infrastructure.persistence.entity.HotelChangeRequestEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class HotelService implements CreateHotelUseCase, UpdateHotelUseCase,
        ApproveHotelUseCase, DeactivateHotelUseCase, DeleteHotelUseCase, QueryHotelUseCase {

    private final HotelRepositoryPort hotelRepository;
    private final HotelEventPublisherPort eventPublisher;
    private final AuditEventPort auditEventPort;
    private final HotelWorkflowSyncService workflowSyncService;
    private final HotelChangeRequestService changeRequestService;

    // ─── CreateHotelUseCase ──────────────────

    @Override
    public Hotel createHotel(Hotel hotel, UUID ownerUserId) {
        if (hotelRepository.existsByNameAndCity(hotel.getName(), hotel.getCity())) {
            throw new CoreException(CoreErrorCode.HOTEL_NAME_DUPLICATE);
        }

        hotel.setOwnerUserId(ownerUserId);
        hotel.setStatus(HotelStatus.PENDING_APPROVAL);

        Hotel saved = hotelRepository.save(hotel);
        log.info("Hotel created: id={}, name={}, owner={}", saved.getId(), saved.getName(), ownerUserId);

        workflowSyncService.createStartRequested(
                saved.getId(),
                null,
                HotelWorkflowType.CREATE_HOTEL,
                saved.getId().toString(),
                saved.getStatus()
        );

        eventPublisher.publishHotelCreated(new CoreDomainEvent.HotelCreated(
                saved.getId(),
                ownerUserId,
                saved.getName(),
                saved.getCity(),
                com.booking.shared.util.SecurityUtils.getCurrentUserEmail(),
                Instant.now()
        ));
        auditEventPort.publish(AuditEventPort.AuditEvent.hotelCreate(
                saved.getId().toString(), ownerUserId.toString(), "HOST", saved.getName()
        ));

        return saved;
    }

    // ─── UpdateHotelUseCase ──────────────────

    @Override
    public Hotel updateHotel(UUID hotelId, Hotel updates, UUID ownerUserId) {
        Hotel hotel = findHotelOrThrow(hotelId);

        if (!hotel.isOwnedBy(ownerUserId)) {
            throw new CoreException(CoreErrorCode.HOTEL_NOT_OWNED);
        }

        if (hotel.getStatus() == HotelStatus.ACTIVE) {
            HotelChangeRequestEntity changeRequest = changeRequestService.createUpdateRequest(hotel, updates, ownerUserId);
            eventPublisher.publishHotelChangeRequested(changeRequestService.toEvent(
                    hotel,
                    changeRequest,
                    com.booking.shared.util.SecurityUtils.getCurrentUserEmail()
            ));
            auditEventPort.publish(new AuditEventPort.AuditEvent(
                    "HOTEL_UPDATE_REQUEST",
                    "HOTEL",
                    hotel.getId().toString(),
                    ownerUserId.toString(),
                    "HOST",
                    "HOST",
                    "Hotel update request submitted",
                    java.util.Map.of("hotelName", hotel.getName(), "changeRequestId", changeRequest.getId().toString()),
                    Instant.now()
            ));
            log.info("Hotel update request submitted: hotelId={}, changeRequestId={}", hotelId, changeRequest.getId());
            return hotel;
        }

        // Check duplicate name if name changed
        if (!hotel.getName().equals(updates.getName())
                && hotelRepository.existsByNameAndCity(updates.getName(), updates.getCity())) {
            throw new CoreException(CoreErrorCode.HOTEL_NAME_DUPLICATE);
        }

        hotel.setName(updates.getName());
        hotel.setDescription(updates.getDescription());
        hotel.setAddress(updates.getAddress());
        hotel.setCity(updates.getCity());
        hotel.setCountry(updates.getCountry());
        hotel.setAmenities(updates.getAmenities());
        hotel.setCheckInTime(updates.getCheckInTime());
        hotel.setCheckOutTime(updates.getCheckOutTime());
        hotel.setImages(updates.getImages());

        Hotel saved = hotelRepository.save(hotel);
        log.info("Hotel updated: id={}, name={}", saved.getId(), saved.getName());

        return saved;
    }

    // ─── ApproveHotelUseCase ─────────────────

    @Override
    public Hotel approveHotel(UUID hotelId) {
        Hotel hotel = findHotelOrThrow(hotelId);

        if (hotel.getStatus() == HotelStatus.ACTIVE) {
            throw new CoreException(CoreErrorCode.HOTEL_ALREADY_APPROVED);
        }

        hotel.approve();

        Hotel saved = hotelRepository.save(hotel);
        log.info("Hotel approved: id={}, name={}", saved.getId(), saved.getName());

        eventPublisher.publishHotelApproved(new CoreDomainEvent.HotelApproved(
                saved.getId(), saved.getName(), Instant.now()
        ));
        auditEventPort.publish(AuditEventPort.AuditEvent.hotelApprove(
                saved.getId().toString(), "SYSTEM", "ADMIN", saved.getName()
        ));

        return saved;
    }

    // ─── QueryHotelUseCase ───────────────────

    public Hotel rejectHotel(UUID hotelId, String reason) {
        Hotel hotel = findHotelOrThrow(hotelId);
        hotel.deactivate();

        Hotel saved = hotelRepository.save(hotel);
        log.info("Hotel rejected: id={}, name={}, reason={}", saved.getId(), saved.getName(), reason);

        auditEventPort.publish(new AuditEventPort.AuditEvent(
                "HOTEL_REJECT",
                "HOTEL",
                saved.getId().toString(),
                "SYSTEM",
                "ADMIN",
                "ADMIN",
                "Hotel '" + saved.getName() + "' rejected",
                java.util.Map.of("hotelName", saved.getName(), "reason", reason),
                Instant.now()
        ));

        return saved;
    }

    public Hotel approveHotelChange(UUID changeRequestId, String reviewerId, String comment) {
        HotelChangeRequestEntity request = changeRequestService.getById(changeRequestId);
        Hotel hotel = findHotelOrThrow(request.getHotelId());
        applyChangeSnapshot(hotel, request.getProposedChanges());

        Hotel saved = hotelRepository.save(hotel);
        changeRequestService.approve(changeRequestId, reviewerId, comment);
        workflowSyncService.markApproved(changeRequestId.toString(), saved.getStatus());

        auditEventPort.publish(new AuditEventPort.AuditEvent(
                "HOTEL_UPDATE_APPROVE",
                "HOTEL",
                saved.getId().toString(),
                reviewerId,
                "ADMIN",
                "ADMIN",
                "Hotel update request approved",
                java.util.Map.of("hotelName", saved.getName(), "changeRequestId", changeRequestId.toString()),
                Instant.now()
        ));
        log.info("Hotel change approved: hotelId={}, changeRequestId={}", saved.getId(), changeRequestId);
        return saved;
    }

    public Hotel rejectHotelChange(UUID changeRequestId, String reviewerId, String reason) {
        HotelChangeRequestEntity request = changeRequestService.reject(changeRequestId, reviewerId, reason);
        Hotel hotel = findHotelOrThrow(request.getHotelId());
        workflowSyncService.markRejected(changeRequestId.toString(), hotel.getStatus(), reason);

        auditEventPort.publish(new AuditEventPort.AuditEvent(
                "HOTEL_UPDATE_REJECT",
                "HOTEL",
                hotel.getId().toString(),
                reviewerId,
                "ADMIN",
                "ADMIN",
                "Hotel update request rejected",
                java.util.Map.of("hotelName", hotel.getName(), "changeRequestId", changeRequestId.toString(), "reason", reason),
                Instant.now()
        ));
        log.info("Hotel change rejected: hotelId={}, changeRequestId={}", hotel.getId(), changeRequestId);
        return hotel;
    }

    @Override
    public Hotel deactivateHotel(UUID hotelId) {
        Hotel hotel = findHotelOrThrow(hotelId);
        return deactivate(hotel, "SYSTEM", "ADMIN", "Hotel deactivated by admin");
    }

    @Override
    public Hotel deactivateOwnHotel(UUID hotelId, UUID ownerUserId) {
        Hotel hotel = findHotelOrThrow(hotelId);
        if (!hotel.isOwnedBy(ownerUserId)) {
            throw new CoreException(CoreErrorCode.HOTEL_NOT_OWNED);
        }
        return deactivate(hotel, ownerUserId.toString(), "HOST", "Hotel deactivated by host");
    }

    private Hotel deactivate(Hotel hotel, String actorId, String actorRole, String description) {
        if (hotel.getStatus() == HotelStatus.INACTIVE) {
            return hotel;
        }

        hotel.deactivate();
        Hotel saved = hotelRepository.save(hotel);
        log.info("Hotel deactivated: id={}, name={}, actor={}", saved.getId(), saved.getName(), actorId);

        eventPublisher.publishHotelDeactivated(new CoreDomainEvent.HotelDeactivated(
                saved.getId(), saved.getName(), Instant.now()
        ));
        auditEventPort.publish(new AuditEventPort.AuditEvent(
                "HOTEL_DEACTIVATE",
                "HOTEL",
                saved.getId().toString(),
                actorId,
                actorRole,
                actorRole,
                description,
                java.util.Map.of("hotelName", saved.getName()),
                Instant.now()
        ));

        return saved;
    }

    @Override
    public void deleteHotel(UUID hotelId) {
        Hotel hotel = findHotelOrThrow(hotelId);
        delete(hotel, "SYSTEM", "ADMIN");
    }

    @Override
    public void deleteOwnHotel(UUID hotelId, UUID ownerUserId) {
        Hotel hotel = findHotelOrThrow(hotelId);
        if (!hotel.isOwnedBy(ownerUserId)) {
            throw new CoreException(CoreErrorCode.HOTEL_NOT_OWNED);
        }
        delete(hotel, ownerUserId.toString(), "HOST");
    }

    private void delete(Hotel hotel, String actorId, String actorRole) {
        if (hotelRepository.existsRoomByHotelId(hotel.getId())
                || hotelRepository.existsBookingByHotelId(hotel.getId())) {
            throw new CoreException(
                    CoreErrorCode.INVALID_REQUEST,
                    "Cannot delete hotel with rooms or bookings. Deactivate it instead."
            );
        }

        hotelRepository.deleteById(hotel.getId());
        log.info("Hotel deleted: id={}, name={}, actor={}", hotel.getId(), hotel.getName(), actorId);

        auditEventPort.publish(new AuditEventPort.AuditEvent(
                "HOTEL_DELETE",
                "HOTEL",
                hotel.getId().toString(),
                actorId,
                actorRole,
                actorRole,
                "Hotel deleted",
                java.util.Map.of("hotelName", hotel.getName()),
                Instant.now()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public Hotel getById(UUID hotelId) {
        return findHotelOrThrow(hotelId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Hotel> getByOwner(UUID ownerUserId, Pageable pageable) {
        return hotelRepository.findByOwnerUserId(ownerUserId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Hotel> getAll(String status, Pageable pageable) {
        return hotelRepository.findAll(status, pageable);
    }

    // ─── Private ─────────────────────────────

    private Hotel findHotelOrThrow(UUID hotelId) {
        return hotelRepository.findById(hotelId)
                .orElseThrow(() -> new CoreException(CoreErrorCode.HOTEL_NOT_FOUND));
    }

    @SuppressWarnings("unchecked")
    private void applyChangeSnapshot(Hotel hotel, Map<String, Object> changes) {
        hotel.setName((String) changes.get("name"));
        hotel.setDescription((String) changes.get("description"));
        hotel.setAddress((String) changes.get("address"));
        hotel.setCity((String) changes.get("city"));
        hotel.setCountry((String) changes.get("country"));

        Object amenities = changes.get("amenities");
        if (amenities instanceof List<?>) {
            hotel.setAmenities(((List<?>) amenities).stream().map(String::valueOf).toList());
        }

        Object images = changes.get("images");
        if (images instanceof List<?>) {
            hotel.setImages(((List<?>) images).stream().map(String::valueOf).toList());
        }

        Object checkInTime = changes.get("checkInTime");
        if (checkInTime instanceof String value && !value.isBlank()) {
            hotel.setCheckInTime(LocalTime.parse(value));
        }

        Object checkOutTime = changes.get("checkOutTime");
        if (checkOutTime instanceof String value && !value.isBlank()) {
            hotel.setCheckOutTime(LocalTime.parse(value));
        }
    }
}
