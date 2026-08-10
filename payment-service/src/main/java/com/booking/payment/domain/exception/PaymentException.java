package com.booking.payment.domain.exception;

import lombok.Getter;

@Getter
public class PaymentException extends RuntimeException {

    private final PaymentErrorCode errorCode;
    private final String detail;

    public PaymentException(PaymentErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = null;
    }

    public PaymentException(PaymentErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + ": " + detail);
        this.errorCode = errorCode;
        this.detail = detail;
    }
}