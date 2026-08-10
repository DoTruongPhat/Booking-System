package com.booking.application.port.in;

import com.booking.domain.enums.CancelledBy;
import com.booking.domain.model.Booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface PaymentBookingSyncUseCase {

    Booking markPaidAndConfirm(UUID bookingId, UUID paymentId, BigDecimal amount, String method, Instant paidAt);

    Booking cancelForPaymentEvent(UUID bookingId, CancelledBy cancelledBy, String reason);

    Booking applyPaymentRefund(UUID bookingId, UUID paymentId, BigDecimal amount);
}
