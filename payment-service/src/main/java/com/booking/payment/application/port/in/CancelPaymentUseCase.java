package com.booking.payment.application.port.in;

import com.booking.payment.domain.model.Payment;

import java.util.UUID;

public interface CancelPaymentUseCase {

    Payment execute(UUID paymentId, UUID userId, String reason);
}