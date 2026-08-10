package com.booking.payment.presentation.controller;

import com.booking.payment.application.port.in.GetPaymentUseCase;
import com.booking.payment.application.port.in.InitRefundUseCase;
import com.booking.payment.application.port.in.VerifyCallbackUseCase;
import com.booking.payment.application.port.out.PaymentGatewayPort;
import com.booking.payment.infrastructure.gateway.PaymentGatewayFactory;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.RefundHistory;
import com.booking.payment.presentation.request.RefundRequest;
import com.booking.payment.presentation.response.ApiResponse;
import com.booking.payment.presentation.response.PaymentResponse;
import com.booking.payment.presentation.response.RefundResponse;
import com.booking.payment.infrastructure.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class PaymentAdminController {

    private final GetPaymentUseCase getPaymentUseCase;
    private final InitRefundUseCase initRefundUseCase;
    private final VerifyCallbackUseCase verifyCallbackUseCase;
    private final PaymentGatewayFactory gatewayFactory;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getAllPayments(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 50) Pageable pageable) {

        Page<PaymentResponse> payments = getPaymentUseCase.getAll(status, pageable)
                .map(PaymentResponse::from);

        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable UUID id) {
        Payment payment = getPaymentUseCase.getById(id);
        return ResponseEntity.ok(ApiResponse.success(PaymentResponse.from(payment)));
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<ApiResponse<PaymentResponse>> syncPayment(@PathVariable UUID id) {
        Payment payment = getPaymentUseCase.getById(id);

        if (payment.getGatewayTxnId() == null || payment.getGatewayTxnId().isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Payment has no gateway transaction id",
                    PaymentResponse.from(payment)
            ));
        }

        if (payment.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Payment already successful",
                    PaymentResponse.from(payment)
            ));
        }

        PaymentGatewayPort.GatewayPaymentStatus gatewayStatus =
                gatewayFactory.getGateway(payment.getMethod()).getPaymentStatus(payment.getGatewayTxnId());

        if (gatewayStatus == PaymentGatewayPort.GatewayPaymentStatus.PAID) {
            Map<String, String> params = new HashMap<>();
            params.put("orderCode", payment.getGatewayTxnId());
            params.put("status", "PAID");
            params.put("code", "00");
            params.put("amount", payment.getAmount().toPlainString());

            verifyCallbackUseCase.execute(payment.getMethod().name(), params);
            payment = getPaymentUseCase.getById(id);

            return ResponseEntity.ok(ApiResponse.success(
                    payment.isSuccess()
                            ? "Payment synced from gateway"
                            : "Gateway is PAID but local payment was not updated",
                    PaymentResponse.from(payment)
            ));
        }

        return ResponseEntity.ok(ApiResponse.success(
                "Gateway status is " + gatewayStatus.name(),
                PaymentResponse.from(payment)
        ));
    }

    @GetMapping("/{id}/refunds")
    public ResponseEntity<ApiResponse<List<RefundResponse>>> getRefundHistory(@PathVariable UUID id) {
        List<RefundResponse> refunds = getPaymentUseCase.getRefundHistory(id).stream()
                .map(RefundResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(refunds));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<RefundResponse>> refundPayment(
            @PathVariable UUID id,
            @Valid @RequestBody RefundRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        UUID adminId = SecurityUtils.getCurrentUserId();

        RefundHistory refund = initRefundUseCase.execute(
                id, request.getAmount(), request.getReason(), adminId, idempotencyKey);

        return ResponseEntity.ok(ApiResponse.success("Refund processed", RefundResponse.from(refund)));
    }
}
