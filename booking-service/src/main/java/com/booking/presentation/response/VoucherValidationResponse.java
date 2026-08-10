package com.booking.presentation.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record VoucherValidationResponse(
        boolean valid,
        String message,
        UUID voucherId,
        String code,
        String discountType,
        BigDecimal discountValue,
        BigDecimal discountAmount
) {}
