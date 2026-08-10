package com.booking.payment.presentation.controller;

import com.booking.payment.application.port.in.VerifyCallbackUseCase;
import com.booking.payment.application.port.out.PaymentGatewayPort;
import com.booking.payment.application.port.out.PaymentRepositoryPort;
import com.booking.payment.infrastructure.gateway.PaymentGatewayFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/callback")
@RequiredArgsConstructor
@Slf4j
public class PaymentCallbackController {

    private final VerifyCallbackUseCase verifyCallbackUseCase;
    private final PaymentRepositoryPort paymentRepository;
    private final PaymentGatewayFactory gatewayFactory;

    @PostMapping("/vnpay")
    public ResponseEntity<String> vnpayCallback(@RequestParam Map<String, String> params) {
        log.info("VNPAY callback received: {}", params.keySet());
        try {
            verifyCallbackUseCase.execute("VNPAY", params);
        } catch (Exception e) {
            log.error("VNPAY callback processing error: {}", e.getMessage());
        }
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/vietqr")
    public ResponseEntity<String> vietqrCallback(@RequestBody(required = false) String body,
                                                 @RequestParam(required = false) Map<String, String> queryParams) {
        log.info("PayOS webhook received");
        try {
            Map<String, String> params = new HashMap<>();

            if (body != null && !body.isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = mapper.readTree(body);

                if (json.has("data")) {
                    JsonNode data = json.get("data");
                    if (data.has("orderCode")) params.put("orderCode", data.get("orderCode").asText());
                    if (data.has("amount")) params.put("amount", data.get("amount").asText());
                    if (data.has("description")) params.put("description", data.get("description").asText());
                }
                if (json.has("code")) params.put("code", json.get("code").asText());
                if (json.has("success")) params.put("status", json.get("success").asBoolean() ? "PAID" : "FAILED");
                if (json.has("signature")) params.put("signature", json.get("signature").asText());

                log.info("PayOS webhook parsed: {}", params);
            }

            if (queryParams != null && !queryParams.isEmpty()) {
                params.putAll(queryParams);
            }

            if (!params.isEmpty()) {
                verifyCallbackUseCase.execute("VIETQR", params);
            }

        } catch (Exception e) {
            log.error("PayOS webhook error: {}", e.getMessage());
        }
        return ResponseEntity.ok("{\"success\":true}");
    }

    @GetMapping("/vietqr/check")
    public ResponseEntity<String> checkPayOSStatus(@RequestParam Map<String, String> requestParams) {
        String orderCode = requestParams.get("orderCode");
        log.info("Checking PayOS status: orderCode={}", orderCode);
        try {
            if (orderCode == null || orderCode.isBlank()) {
                return ResponseEntity.ok("{\"success\":false,\"reason\":\"orderCode is required\"}");
            }

            var payment = paymentRepository.findByGatewayTxnId(orderCode);
            if (payment.isEmpty()) {
                return ResponseEntity.ok("{\"success\":false,\"reason\":\"payment not found\"}");
            }
            if (requestParams.containsKey("bookingId")
                    && !payment.get().getBookingId().toString().equals(requestParams.get("bookingId"))) {
                return ResponseEntity.ok("{\"success\":false,\"reason\":\"booking mismatch\"}");
            }

            if ("SUCCESS".equals(payment.get().getStatus().name())) {
                return ResponseEntity.ok("{\"success\":true,\"status\":\"SUCCESS\"}");
            }

            PaymentGatewayPort.GatewayPaymentStatus gatewayStatus =
                    gatewayFactory.getGateway("VIETQR").getPaymentStatus(orderCode);
            boolean returnUrlSaysPaid = "00".equals(requestParams.get("code"))
                    || "PAID".equalsIgnoreCase(requestParams.get("status"));
            if (gatewayStatus != PaymentGatewayPort.GatewayPaymentStatus.PAID && !returnUrlSaysPaid) {
                return ResponseEntity.ok("{\"success\":false,\"status\":\"" + gatewayStatus.name() + "\"}");
            }

            Map<String, String> params = new HashMap<>();
            params.put("orderCode", orderCode);
            params.put("status", "PAID");
            params.put("code", "00");
            params.put("amount", payment.get().getAmount().toString());

            verifyCallbackUseCase.execute("VIETQR", params);
            return ResponseEntity.ok("{\"success\":true,\"status\":\"SUCCESS\"}");
        } catch (Exception e) {
            log.error("PayOS check error: {}", e.getMessage());
            return ResponseEntity.ok("{\"success\":false,\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
