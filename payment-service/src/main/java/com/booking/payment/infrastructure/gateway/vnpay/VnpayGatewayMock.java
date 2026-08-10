package com.booking.payment.infrastructure.gateway.vnpay;

import com.booking.payment.application.port.out.PaymentGatewayPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class VnpayGatewayMock implements PaymentGatewayPort {

    @Value("${server.port:8083}")
    private int serverPort;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "VNPAY";
    }

    @Override
    public InitPaymentResult initPayment(InitPaymentRequest request) {
        String gatewayTxnId = "VNP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String paymentUrl = "http://localhost:" + serverPort +
                "/mock-gateway/vnpay?txn=" + gatewayTxnId +
                "&amount=" + request.amount().toBigInteger() +
                "&bookingId=" + request.bookingId();

        log.info("VNPAY Mock: initiated txn={}, amount={}", gatewayTxnId, request.amount());

        return new InitPaymentResult(
                true,
                gatewayTxnId,
                paymentUrl,
                "{\"mock\":true,\"txn\":\"" + gatewayTxnId + "\"}"
        );
    }

    @Override
    public CallbackVerifyResult verifyCallback(Map<String, String> params) {
        String txnId = params.getOrDefault("vnp_TxnRef", "");
        String responseCode = params.getOrDefault("vnp_ResponseCode", "00");
        String amountStr = params.getOrDefault("vnp_Amount", "0");

        boolean success = "00".equals(responseCode);

        log.info("VNPAY Mock: verify txn={}, code={}, success={}", txnId, responseCode, success);

        return new CallbackVerifyResult(
                success, true, txnId,
                new BigDecimal(amountStr), responseCode, toJson(params)
        );
    }

    @Override
    public RefundResult refund(RefundRequest request) {
        String refundTxnId = "VNP-RF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("VNPAY Mock: refund txn={}, amount={}", request.gatewayTxnId(), request.amount());
        return new RefundResult(true, refundTxnId, "{\"mock\":true}");
    }

    @Override
    public void cancelPayment(String gatewayTxnId) {
        log.info("VNPAY Mock: cancel txn={}", gatewayTxnId);
    }

    private String toJson(Map<String, String> params) {
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            return "{\"raw\":\"" + escapeJson(params.toString()) + "\"}";
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
