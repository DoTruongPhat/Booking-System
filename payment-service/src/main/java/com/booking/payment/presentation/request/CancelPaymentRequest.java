package com.booking.payment.presentation.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelPaymentRequest {
    private String reason;
}