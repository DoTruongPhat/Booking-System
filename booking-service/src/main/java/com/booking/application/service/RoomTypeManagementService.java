package com.booking.application.service;

import com.booking.domain.exception.CoreErrorCode;
import com.booking.domain.exception.CoreException;
import com.booking.infrastructure.persistence.entity.HotelEntity;
import com.booking.infrastructure.persistence.entity.RoomTypeEntity;
import com.booking.infrastructure.persistence.repository.HotelJpaRepository;
import com.booking.infrastructure.persistence.repository.RoomTypeJpaRepository;
import com.booking.presentation.request.RoomTypeRequest;
import com.booking.presentation.response.RoomTypeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomTypeManagementService {

    private final RoomTypeJpaRepository roomTypeRepository;
    private final HotelJpaRepository hotelRepository;

    @Transactional(readOnly = true)
    public Page<RoomTypeResponse> findForAdmin(UUID hotelId, Boolean active, Pageable pageable) {
        return roomTypeRepository.findForAdmin(hotelId, active, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<RoomTypeResponse> findForHost(UUID ownerUserId, Boolean active, Pageable pageable) {
        return roomTypeRepository.findForHost(ownerUserId, active, pageable).map(this::toResponse);
    }

    public RoomTypeResponse createForAdmin(RoomTypeRequest request) {
        HotelEntity hotel = request.getHotelId() == null ? null : findHotel(request.getHotelId());
        ensureUnique(null, hotel, request.getCode());
        return toResponse(roomTypeRepository.save(toEntity(new RoomTypeEntity(), hotel, request)));
    }

    public RoomTypeResponse createForHost(UUID ownerUserId, RoomTypeRequest request) {
        HotelEntity hotel = findOwnedHotel(requiredHotelId(request.getHotelId()), ownerUserId);
        ensureUnique(null, hotel, request.getCode());
        return toResponse(roomTypeRepository.save(toEntity(new RoomTypeEntity(), hotel, request)));
    }

    public RoomTypeResponse updateForAdmin(UUID id, RoomTypeRequest request) {
        RoomTypeEntity entity = findRoomType(id);
        HotelEntity hotel = request.getHotelId() == null ? null : findHotel(request.getHotelId());
        ensureUnique(id, hotel, request.getCode());
        return toResponse(roomTypeRepository.save(toEntity(entity, hotel, request)));
    }

    public RoomTypeResponse updateForHost(UUID ownerUserId, UUID id, RoomTypeRequest request) {
        RoomTypeEntity entity = findRoomType(id);
        assertOwned(entity, ownerUserId);
        HotelEntity hotel = findOwnedHotel(requiredHotelId(request.getHotelId()), ownerUserId);
        ensureUnique(id, hotel, request.getCode());
        return toResponse(roomTypeRepository.save(toEntity(entity, hotel, request)));
    }

    public void deleteForAdmin(UUID id) {
        roomTypeRepository.delete(findRoomType(id));
    }

    public void deleteForHost(UUID ownerUserId, UUID id) {
        RoomTypeEntity entity = findRoomType(id);
        assertOwned(entity, ownerUserId);
        roomTypeRepository.delete(entity);
    }

    private RoomTypeEntity toEntity(RoomTypeEntity entity, HotelEntity hotel, RoomTypeRequest request) {
        entity.setHotel(hotel);
        entity.setCode(normalizeCode(request.getCode()));
        entity.setName(request.getName().trim());
        entity.setDescription(request.getDescription());
        entity.setDefaultCapacity(request.getDefaultCapacity());
        entity.setDefaultAmenities(request.getDefaultAmenities() == null ? List.of() : request.getDefaultAmenities());
        entity.setActive(request.getActive() == null || request.getActive());
        return entity;
    }

    private RoomTypeResponse toResponse(RoomTypeEntity entity) {
        HotelEntity hotel = entity.getHotel();
        return RoomTypeResponse.builder()
                .id(entity.getId())
                .hotelId(hotel == null ? null : hotel.getId())
                .hotelName(hotel == null ? null : hotel.getName())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .defaultCapacity(entity.getDefaultCapacity())
                .defaultAmenities(entity.getDefaultAmenities())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private void ensureUnique(UUID currentId, HotelEntity hotel, String code) {
        UUID hotelId = hotel == null ? null : hotel.getId();
        roomTypeRepository.findByScopeAndCode(hotelId, normalizeCode(code))
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new CoreException(CoreErrorCode.ROOM_TYPE_DUPLICATE);
                });
    }

    private RoomTypeEntity findRoomType(UUID id) {
        return roomTypeRepository.findById(id)
                .orElseThrow(() -> new CoreException(CoreErrorCode.ROOM_TYPE_NOT_FOUND));
    }

    private HotelEntity findHotel(UUID hotelId) {
        return hotelRepository.findById(hotelId)
                .orElseThrow(() -> new CoreException(CoreErrorCode.HOTEL_NOT_FOUND));
    }

    private HotelEntity findOwnedHotel(UUID hotelId, UUID ownerUserId) {
        HotelEntity hotel = findHotel(hotelId);
        if (!hotel.getOwnerUserId().equals(ownerUserId)) {
            throw new CoreException(CoreErrorCode.HOTEL_NOT_OWNED);
        }
        return hotel;
    }

    private void assertOwned(RoomTypeEntity entity, UUID ownerUserId) {
        if (entity.getHotel() == null || !entity.getHotel().getOwnerUserId().equals(ownerUserId)) {
            throw new CoreException(CoreErrorCode.HOTEL_NOT_OWNED);
        }
    }

    private UUID requiredHotelId(UUID hotelId) {
        if (hotelId == null) {
            throw new CoreException(CoreErrorCode.INVALID_REQUEST, "hotelId is required for host resources");
        }
        return hotelId;
    }

    private String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase().replaceAll("\\s+", "_");
    }
}
