package com.booking.payment.presentation.response;

import com.booking.payment.domain.model.Payment;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class PaymentResponse {

    private UUID id;
    private String paymentCode;
    private UUID bookingId;
    private UUID userId;
    private BigDecimal amount;
    private String currency;
    private String method;
    private String status;
    private String gatewayTxnId;
    private String gatewayUrl;
    private Instant initiatedAt;
    private Instant completedAt;
    private Instant expiresAt;
    private Instant createdAt;

    public static PaymentResponse from(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .paymentCode(p.getPaymentCode())
                .bookingId(p.getBookingId())
                .userId(p.getUserId())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .method(p.getMethod().name())
                .status(p.getStatus().name())
                .gatewayTxnId(p.getGatewayTxnId())
                .gatewayUrl(p.getGatewayUrl())
                .initiatedAt(p.getInitiatedAt())
                .completedAt(p.getCompletedAt())
                .expiresAt(p.getExpiresAt())
                .createdAt(p.getCreatedAt())
                .build();
    }
}