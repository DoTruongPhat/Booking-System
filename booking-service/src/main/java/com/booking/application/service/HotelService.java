package com.booking.application.service;

import com.booking.application.port.in.ApproveHotelUseCase;
import com.booking.application.port.in.CreateHotelUseCase;
import com.booking.application.port.in.QueryHotelUseCase;
import com.booking.application.port.in.UpdateHotelUseCase;
import com.booking.application.port.out.AuditEventPort;
import com.booking.application.port.out.HotelEventPublisherPort;
import com.booking.application.port.out.HotelRepositoryPort;
import com.booking.domain.enums.HotelStatus;
import com.booking.domain.event.CoreDomainEvent;
import com.booking.domain.exception.CoreErrorCode;
import com.booking.domain.exception.CoreException;
import com.booking.domain.model.Hotel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class HotelService implements CreateHotelUseCase, UpdateHotelUseCase,
        ApproveHotelUseCase, QueryHotelUseCase {

    private final HotelRepositoryPort hotelRepository;
    private final HotelEventPublisherPort eventPublisher;
    private final AuditEventPort auditEventPort;

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

        eventPublisher.publishHotelCreated(new CoreDomainEvent.HotelCreated(
                saved.getId(), ownerUserId, saved.getName(), saved.getCity(), Instant.now()
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
}