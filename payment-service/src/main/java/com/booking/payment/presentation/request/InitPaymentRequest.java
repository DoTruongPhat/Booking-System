package com.booking.payment.presentation.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class InitPaymentRequest {

    @NotNull(message = "Booking ID is required")
    private UUID bookingId;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotNull(message = "Payment method is required")
    private String method;

    private String currency;
}