package com.booking.payment.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode {

    PAYMENT_NOT_FOUND("PAY_001", "Payment not found", HttpStatus.NOT_FOUND),
    PAYMENT_ALREADY_EXISTS("PAY_002", "Active payment already exists for this booking", HttpStatus.CONFLICT),
    PAYMENT_NOT_PENDING("PAY_003", "Payment is not in PENDING status", HttpStatus.BAD_REQUEST),
    PAYMENT_NOT_SUCCESS("PAY_004", "Payment is not in SUCCESS status", HttpStatus.BAD_REQUEST),
    PAYMENT_EXPIRED("PAY_005", "Payment has expired", HttpStatus.BAD_REQUEST),
    PAYMENT_AMOUNT_MISMATCH("PAY_006", "Callback amount does not match payment amount", HttpStatus.BAD_REQUEST),
    PAYMENT_INVALID_SIGNATURE("PAY_007", "Invalid gateway signature", HttpStatus.BAD_REQUEST),
    PAYMENT_GATEWAY_ERROR("PAY_008", "Payment gateway error", HttpStatus.SERVICE_UNAVAILABLE),

    REFUND_EXCEEDS_AMOUNT("PAY_010", "Refund amount exceeds refundable amount", HttpStatus.BAD_REQUEST),
    REFUND_FAILED("PAY_011", "Refund processing failed", HttpStatus.INTERNAL_SERVER_ERROR),
    REFUND_DUPLICATE("PAY_012", "Duplicate refund request", HttpStatus.CONFLICT),

    GATEWAY_NOT_SUPPORTED("PAY_020", "Payment gateway not supported", HttpStatus.BAD_REQUEST),
    GATEWAY_TIMEOUT("PAY_021", "Payment gateway timeout", HttpStatus.GATEWAY_TIMEOUT),

    IDEMPOTENCY_KEY_REUSED("PAY_030", "Idempotency key already used", HttpStatus.CONFLICT),

    PAYMENT_NOT_OWNED("PAY_040", "You do not own this payment", HttpStatus.FORBIDDEN),
    BOOKING_NOT_FOUND("PAY_050", "Booking not found or cannot be validated", HttpStatus.NOT_FOUND),
    BOOKING_NOT_PAYABLE("PAY_051", "Booking is not payable", HttpStatus.BAD_REQUEST),

    INVALID_REQUEST("PAY_900", "Invalid request", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("PAY_999", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
