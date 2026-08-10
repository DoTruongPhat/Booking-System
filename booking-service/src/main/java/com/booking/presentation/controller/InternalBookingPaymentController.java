package com.booking.presentation.controller;

import com.booking.application.port.in.QueryBookingUseCase;
import com.booking.application.service.PaymentDeadlinePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/internal/bookings")
@RequiredArgsConstructor
public class InternalBookingPaymentController {

    private final QueryBookingUseCase queryBookingUseCase;
    private final PaymentDeadlinePolicy paymentDeadlinePolicy;

    @GetMapping("/{bookingId}/payment-snapshot")
    public PaymentSnapshotResponse getPaymentSnapshot(@PathVariable UUID bookingId) {
        var booking = queryBookingUseCase.getById(bookingId);
        return new PaymentSnapshotResponse(
                booking.getId(),
                booking.getUserId(),
                booking.getTotalPrice(),
                booking.getStatus().name(),
                booking.getPaymentStatus().name(),
                "VND",
                paymentDeadlinePolicy.expiresAt(booking)
        );
    }

    public record PaymentSnapshotResponse(
            UUID bookingId,
            UUID userId,
            BigDecimal totalPrice,
            String status,
            String paymentStatus,
            String currency,
            java.time.Instant paymentExpiresAt
    ) {}
}
