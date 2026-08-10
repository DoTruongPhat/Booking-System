package com.booking.application.service;

import com.booking.domain.exception.CoreErrorCode;
import com.booking.domain.exception.CoreException;
import com.booking.infrastructure.persistence.entity.HotelEntity;
import com.booking.infrastructure.persistence.entity.PromotionEntity;
import com.booking.infrastructure.persistence.repository.HotelJpaRepository;
import com.booking.infrastructure.persistence.repository.PromotionJpaRepository;
import com.booking.presentation.request.PromotionRequest;
import com.booking.presentation.response.PromotionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PromotionManagementService {

    private final PromotionJpaRepository promotionRepository;
    private final HotelJpaRepository hotelRepository;

    @Transactional(readOnly = true)
    public Page<PromotionResponse> findForAdmin(UUID hotelId, Boolean active, Pageable pageable) {
        return promotionRepository.findForAdmin(hotelId, active, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<PromotionResponse> findForHost(UUID ownerUserId, Boolean active, Pageable pageable) {
        return promotionRepository.findForHost(ownerUserId, active, pageable).map(this::toResponse);
    }

    public PromotionResponse createForAdmin(PromotionRequest request) {
        validateDates(request);
        HotelEntity hotel = request.getHotelId() == null ? null : findHotel(request.getHotelId());
        return toResponse(promotionRepository.save(toEntity(new PromotionEntity(), hotel, request)));
    }

    public PromotionResponse createForHost(UUID ownerUserId, PromotionRequest request) {
        validateDates(request);
        HotelEntity hotel = findOwnedHotel(requiredHotelId(request.getHotelId()), ownerUserId);
        return toResponse(promotionRepository.save(toEntity(new PromotionEntity(), hotel, request)));
    }

    public PromotionResponse updateForAdmin(UUID id, PromotionRequest request) {
        validateDates(request);
        PromotionEntity entity = findPromotion(id);
        HotelEntity hotel = request.getHotelId() == null ? null : findHotel(request.getHotelId());
        return toResponse(promotionRepository.save(toEntity(entity, hotel, request)));
    }

    public PromotionResponse updateForHost(UUID ownerUserId, UUID id, PromotionRequest request) {
        validateDates(request);
        PromotionEntity entity = findPromotion(id);
        assertOwned(entity, ownerUserId);
        HotelEntity hotel = findOwnedHotel(requiredHotelId(request.getHotelId()), ownerUserId);
        return toResponse(promotionRepository.save(toEntity(entity, hotel, request)));
    }

    public void deleteForAdmin(UUID id) {
        promotionRepository.delete(findPromotion(id));
    }

    public void deleteForHost(UUID ownerUserId, UUID id) {
        PromotionEntity entity = findPromotion(id);
        assertOwned(entity, ownerUserId);
        promotionRepository.delete(entity);
    }

    private PromotionEntity toEntity(PromotionEntity entity, HotelEntity hotel, PromotionRequest request) {
        entity.setHotel(hotel);
        entity.setTitle(request.getTitle().trim());
        entity.setDescription(request.getDescription());
        entity.setDiscountType(request.getDiscountType().trim().toUpperCase());
        entity.setDiscountValue(request.getDiscountValue());
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setActive(request.getActive() == null || request.getActive());
        return entity;
    }

    private PromotionResponse toResponse(PromotionEntity entity) {
        HotelEntity hotel = entity.getHotel();
        return PromotionResponse.builder()
                .id(entity.getId())
                .hotelId(hotel == null ? null : hotel.getId())
                .hotelName(hotel == null ? null : hotel.getName())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .discountType(entity.getDiscountType())
                .discountValue(entity.getDiscountValue())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private PromotionEntity findPromotion(UUID id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new CoreException(CoreErrorCode.PROMOTION_NOT_FOUND));
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

    private void assertOwned(PromotionEntity entity, UUID ownerUserId) {
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

    private void validateDates(PromotionRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new CoreException(CoreErrorCode.INVALID_REQUEST, "endDate must be on or after startDate");
        }
    }
}
