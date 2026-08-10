package com.booking.payment.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface InitPaymentUseCase {

    InitPaymentResult execute(InitPaymentCommand command);

    record InitPaymentCommand(
            UUID bookingId,
            UUID userId,
            BigDecimal amount,
            String method,
            String currency,
            String idempotencyKey
    ) {}

    record InitPaymentResult(
            UUID paymentId,
            String paymentCode,
            String paymentUrl,
            Instant expiresAt
    ) {}
}