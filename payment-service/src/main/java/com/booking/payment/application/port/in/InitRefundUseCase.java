package com.booking.payment.application.port.in;

import com.booking.payment.domain.model.RefundHistory;

import java.math.BigDecimal;
import java.util.UUID;

public interface InitRefundUseCase {

    RefundHistory execute(UUID paymentId, BigDecimal amount,
                          String reason, UUID requestedBy, String idempotencyKey);
}