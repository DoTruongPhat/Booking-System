package com.booking.payment.presentation.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class InitPaymentResponse {

    private UUID paymentId;
    private String paymentCode;
    private String paymentUrl;
    private Instant expiresAt;
}