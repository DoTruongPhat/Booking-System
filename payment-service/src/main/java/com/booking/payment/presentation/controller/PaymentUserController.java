package com.booking.payment.presentation.controller;

import com.booking.payment.application.port.in.CancelPaymentUseCase;
import com.booking.payment.application.port.in.GetPaymentUseCase;
import com.booking.payment.application.port.in.InitPaymentUseCase;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.presentation.request.CancelPaymentRequest;
import com.booking.payment.presentation.request.InitPaymentRequest;
import com.booking.payment.presentation.response.ApiResponse;
import com.booking.payment.presentation.response.InitPaymentResponse;
import com.booking.payment.presentation.response.PaymentResponse;
import com.booking.payment.infrastructure.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/user/payments")
@RequiredArgsConstructor
public class PaymentUserController {

    private final InitPaymentUseCase initPaymentUseCase;
    private final CancelPaymentUseCase cancelPaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;

    @PostMapping("/init")
    public ResponseEntity<ApiResponse<InitPaymentResponse>> initPayment(
            @Valid @RequestBody InitPaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        UUID userId = SecurityUtils.getCurrentUserId();

        var result = initPaymentUseCase.execute(new InitPaymentUseCase.InitPaymentCommand(
                request.getBookingId(),
                userId,
                request.getAmount(),
                request.getMethod(),
                request.getCurrency(),
                idempotencyKey
        ));

        var response = new InitPaymentResponse(
                result.paymentId(),
                result.paymentCode(),
                result.paymentUrl(),
                result.expiresAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment initiated", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByBooking(
            @RequestParam UUID bookingId) {

        UUID userId = SecurityUtils.getCurrentUserId();
        Payment payment = getPaymentUseCase.getByBookingId(bookingId, userId);

        return ResponseEntity.ok(ApiResponse.success(PaymentResponse.from(payment)));
    }

    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancelPayment(
            @PathVariable UUID paymentId,
            @RequestBody(required = false) CancelPaymentRequest request) {

        UUID userId = SecurityUtils.getCurrentUserId();
        String reason = request != null ? request.getReason() : null;

        Payment cancelled = cancelPaymentUseCase.execute(paymentId, userId, reason);

        return ResponseEntity.ok(ApiResponse.success("Payment cancelled", PaymentResponse.from(cancelled)));
    }
}