package com.booking.presentation.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Builder
public record VoucherResponse(
        UUID id,
        UUID hotelId,
        String hotelName,
        String code,
        String description,
        String discountType,
        BigDecimal discountValue,
        BigDecimal minOrderAmount,
        BigDecimal maxDiscountAmount,
        Integer usageLimit,
        Integer usedCount,
        LocalDate startDate,
        LocalDate endDate,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
