package com.booking.application.service;

import com.booking.domain.exception.CoreErrorCode;
import com.booking.domain.exception.CoreException;
import com.booking.infrastructure.persistence.entity.HotelEntity;
import com.booking.infrastructure.persistence.entity.VoucherEntity;
import com.booking.infrastructure.persistence.repository.HotelJpaRepository;
import com.booking.infrastructure.persistence.repository.VoucherJpaRepository;
import com.booking.presentation.request.VoucherRequest;
import com.booking.presentation.response.VoucherResponse;
import com.booking.presentation.response.VoucherValidationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class VoucherManagementService {

    private final VoucherJpaRepository voucherRepository;
    private final HotelJpaRepository hotelRepository;

    @Transactional(readOnly = true)
    public Page<VoucherResponse> findForAdmin(UUID hotelId, Boolean active, Pageable pageable) {
        return voucherRepository.findForAdmin(hotelId, active, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<VoucherResponse> findForHost(UUID ownerUserId, Boolean active, Pageable pageable) {
        return voucherRepository.findForHost(ownerUserId, active, pageable).map(this::toResponse);
    }

    public VoucherResponse createForAdmin(VoucherRequest request) {
        validate(request);
        HotelEntity hotel = request.getHotelId() == null ? null : findHotel(request.getHotelId());
        ensureUnique(null, request.getCode());
        return toResponse(voucherRepository.save(toEntity(new VoucherEntity(), hotel, request)));
    }

    public VoucherResponse createForHost(UUID ownerUserId, VoucherRequest request) {
        validate(request);
        HotelEntity hotel = findOwnedHotel(requiredHotelId(request.getHotelId()), ownerUserId);
        ensureUnique(null, request.getCode());
        return toResponse(voucherRepository.save(toEntity(new VoucherEntity(), hotel, request)));
    }

    public VoucherResponse updateForAdmin(UUID id, VoucherRequest request) {
        validate(request);
        VoucherEntity entity = findVoucher(id);
        HotelEntity hotel = request.getHotelId() == null ? null : findHotel(request.getHotelId());
        ensureUnique(id, request.getCode());
        return toResponse(voucherRepository.save(toEntity(entity, hotel, request)));
    }

    public VoucherResponse updateForHost(UUID ownerUserId, UUID id, VoucherRequest request) {
        validate(request);
        VoucherEntity entity = findVoucher(id);
        assertOwned(entity, ownerUserId);
        HotelEntity hotel = findOwnedHotel(requiredHotelId(request.getHotelId()), ownerUserId);
        ensureUnique(id, request.getCode());
        return toResponse(voucherRepository.save(toEntity(entity, hotel, request)));
    }

    public void deleteForAdmin(UUID id) {
        voucherRepository.delete(findVoucher(id));
    }

    public void deleteForHost(UUID ownerUserId, UUID id) {
        VoucherEntity entity = findVoucher(id);
        assertOwned(entity, ownerUserId);
        voucherRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public VoucherValidationResponse validate(String code, UUID hotelId, BigDecimal orderAmount) {
        BigDecimal amount = orderAmount == null ? BigDecimal.ZERO : orderAmount;
        return voucherRepository.findByCodeIgnoreCase(normalizeCode(code))
                .map(voucher -> validateVoucher(voucher, hotelId, amount))
                .orElseGet(() -> invalid("Voucher code not found"));
    }

    public VoucherValidationResponse redeem(String code, UUID hotelId, BigDecimal orderAmount) {
        BigDecimal amount = orderAmount == null ? BigDecimal.ZERO : orderAmount;
        VoucherEntity voucher = voucherRepository.findByCodeIgnoreCase(normalizeCode(code))
                .orElse(null);
        if (voucher == null) {
            return invalid("Voucher code not found");
        }

        VoucherValidationResponse validation = validateVoucher(voucher, hotelId, amount);
        if (validation.valid()) {
            voucher.setUsedCount((voucher.getUsedCount() == null ? 0 : voucher.getUsedCount()) + 1);
            voucherRepository.save(voucher);
        }
        return validation;
    }

    private VoucherEntity toEntity(VoucherEntity entity, HotelEntity hotel, VoucherRequest request) {
        entity.setHotel(hotel);
        entity.setCode(normalizeCode(request.getCode()));
        entity.setDescription(request.getDescription());
        entity.setDiscountType(request.getDiscountType().trim().toUpperCase());
        entity.setDiscountValue(request.getDiscountValue());
        entity.setMinOrderAmount(request.getMinOrderAmount() == null ? BigDecimal.ZERO : request.getMinOrderAmount());
        entity.setMaxDiscountAmount(request.getMaxDiscountAmount());
        entity.setUsageLimit(request.getUsageLimit());
        if (entity.getUsedCount() == null) {
            entity.setUsedCount(0);
        }
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setActive(request.getActive() == null || request.getActive());
        return entity;
    }

    private VoucherResponse toResponse(VoucherEntity entity) {
        HotelEntity hotel = entity.getHotel();
        return VoucherResponse.builder()
                .id(entity.getId())
                .hotelId(hotel == null ? null : hotel.getId())
                .hotelName(hotel == null ? null : hotel.getName())
                .code(entity.getCode())
                .description(entity.getDescription())
                .discountType(entity.getDiscountType())
                .discountValue(entity.getDiscountValue())
                .minOrderAmount(entity.getMinOrderAmount())
                .maxDiscountAmount(entity.getMaxDiscountAmount())
                .usageLimit(entity.getUsageLimit())
                .usedCount(entity.getUsedCount())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private void ensureUnique(UUID currentId, String code) {
        voucherRepository.findByCodeIgnoreCase(normalizeCode(code))
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new CoreException(CoreErrorCode.VOUCHER_CODE_DUPLICATE);
                });
    }

    private VoucherEntity findVoucher(UUID id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new CoreException(CoreErrorCode.VOUCHER_NOT_FOUND));
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

    private void assertOwned(VoucherEntity entity, UUID ownerUserId) {
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

    private void validate(VoucherRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new CoreException(CoreErrorCode.INVALID_REQUEST, "endDate must be on or after startDate");
        }
    }

    private VoucherValidationResponse validateVoucher(VoucherEntity voucher, UUID hotelId, BigDecimal amount) {
        LocalDate today = LocalDate.now();
        if (!Boolean.TRUE.equals(voucher.getActive())) {
            return invalid("Voucher is inactive");
        }
        if (today.isBefore(voucher.getStartDate()) || today.isAfter(voucher.getEndDate())) {
            return invalid("Voucher is outside its valid date range");
        }
        if (voucher.getHotel() != null && hotelId != null && !voucher.getHotel().getId().equals(hotelId)) {
            return invalid("Voucher is not applicable to this hotel");
        }
        if (voucher.getHotel() != null && hotelId == null) {
            return invalid("hotelId is required for this voucher");
        }
        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            return invalid("Voucher usage limit reached");
        }
        if (voucher.getMinOrderAmount() != null && amount.compareTo(voucher.getMinOrderAmount()) < 0) {
            return invalid("Order amount is below voucher minimum");
        }

        BigDecimal discountAmount = calculateDiscount(voucher, amount);
        return VoucherValidationResponse.builder()
                .valid(true)
                .message("Voucher is valid")
                .voucherId(voucher.getId())
                .code(voucher.getCode())
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .discountAmount(discountAmount)
                .build();
    }

    private BigDecimal calculateDiscount(VoucherEntity voucher, BigDecimal amount) {
        BigDecimal discount = "PERCENT".equals(voucher.getDiscountType())
                ? amount.multiply(voucher.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : voucher.getDiscountValue();
        if (voucher.getMaxDiscountAmount() != null && discount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
            return voucher.getMaxDiscountAmount();
        }
        return discount.min(amount).max(BigDecimal.ZERO);
    }

    private VoucherValidationResponse invalid(String message) {
        return VoucherValidationResponse.builder()
                .valid(false)
                .message(message)
                .discountAmount(BigDecimal.ZERO)
                .build();
    }

    private String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
