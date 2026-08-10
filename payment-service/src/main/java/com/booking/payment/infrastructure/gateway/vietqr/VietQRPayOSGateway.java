package com.booking.payment.infrastructure.gateway.vietqr;

import com.booking.payment.application.port.out.PaymentGatewayPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@Slf4j
public class VietQRPayOSGateway implements PaymentGatewayPort {

    private static final String PAYOS_API = "https://api-merchant.payos.vn/v2/payment-requests";

    @Value("${app.gateway.payos.client-id}")
    private String clientId;

    @Value("${app.gateway.payos.api-key}")
    private String apiKey;

    @Value("${app.gateway.payos.checksum-key}")
    private String checksumKey;

    @Value("${app.gateway.payos.return-url}")
    private String returnUrl;

    @Value("${app.gateway.payos.cancel-url}")
    private String cancelUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "VIETQR";
    }

    @Override
    public InitPaymentResult initPayment(InitPaymentRequest request) {
        try {
            // PayOS orderCode phải là số, dùng timestamp + random
            long orderCode = System.currentTimeMillis() % 1000000000L;

            int amount = request.amount().intValue();
            String description = "Booking " + request.bookingId().toString().substring(0, 8);
            String paymentReturnUrl = appendQuery(returnUrl, "bookingId", request.bookingId().toString());
            String paymentCancelUrl = appendQuery(cancelUrl, "bookingId", request.bookingId().toString());

            // Tạo checksum: amount + cancelUrl + description + orderCode + returnUrl
            String rawData = "amount=" + amount
                    + "&cancelUrl=" + paymentCancelUrl
                    + "&description=" + description
                    + "&orderCode=" + orderCode
                    + "&returnUrl=" + paymentReturnUrl;

            String checksum = hmacSHA256(checksumKey, rawData);

            // Build request body
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("orderCode", orderCode);
            body.put("amount", amount);
            body.put("description", description);
            body.put("cancelUrl", paymentCancelUrl);
            body.put("returnUrl", paymentReturnUrl);
            body.put("signature", checksum);

            // Call PayOS API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);

            String jsonBody = objectMapper.writeValueAsString(body);
            log.info("PayOS request: orderCode={}, amount={}, description={}", orderCode, amount, description);

            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    PAYOS_API, HttpMethod.POST, entity, String.class);

            JsonNode responseJson = objectMapper.readTree(response.getBody());
            String code = responseJson.get("code").asText();

            if ("00".equals(code)) {
                JsonNode data = responseJson.get("data");
                String checkoutUrl = data.get("checkoutUrl").asText();
                String paymentLinkId = data.get("paymentLinkId").asText();
                String qrCode = data.has("qrCode") ? data.get("qrCode").asText() : null;

                log.info("PayOS success: orderCode={}, paymentLinkId={}, checkoutUrl={}",
                        orderCode, paymentLinkId, checkoutUrl);

                return new InitPaymentResult(
                        true,
                        String.valueOf(orderCode),
                        checkoutUrl,
                        response.getBody()
                );
            } else {
                String desc = responseJson.has("desc") ? responseJson.get("desc").asText() : "Unknown error";
                log.error("PayOS failed: code={}, desc={}", code, desc);
                return new InitPaymentResult(false, null, null, response.getBody());
            }

        } catch (Exception e) {
            log.error("PayOS initPayment error: {}", e.getMessage(), e);
            return new InitPaymentResult(false, null, null, e.getMessage());
        }
    }

    @Override
    public CallbackVerifyResult verifyCallback(Map<String, String> params) {
        try {
            String code = params.getOrDefault("code", "");
            String orderCodeStr = params.getOrDefault("orderCode", "");
            String status = params.getOrDefault("status", "");
            String amountStr = params.get("amount");

            // Nếu là webhook từ PayOS (JSON body đã parse thành params)
            boolean success = "00".equals(code) || "PAID".equalsIgnoreCase(status);

            // Verify signature nếu có
            boolean signatureValid = true;
            String signature = params.getOrDefault("signature", "");
            if (!signature.isEmpty() && !orderCodeStr.isEmpty()) {
                String rawData = "orderCode=" + orderCodeStr;
                String expectedSignature = hmacSHA256(checksumKey, rawData);
                signatureValid = expectedSignature.equals(signature);

                if (!signatureValid) {
                    log.warn("PayOS signature mismatch: expected={}, got={}", expectedSignature, signature);
                }
            }

            BigDecimal amount = amountStr == null || amountStr.isBlank() ? null : new BigDecimal(amountStr);

            log.info("PayOS callback verify: orderCode={}, status={}, success={}, signatureValid={}",
                    orderCodeStr, status, success, signatureValid);

            return new CallbackVerifyResult(
                    success,
                    signatureValid,
                    orderCodeStr,
                    amount,
                    code,
                    toJson(params)
            );

        } catch (Exception e) {
            log.error("PayOS verifyCallback error: {}", e.getMessage());
            return new CallbackVerifyResult(false, false, "", BigDecimal.ZERO, "ERROR",
                    "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    @Override
    public RefundResult refund(RefundRequest request) {
        // PayOS không hỗ trợ auto refund qua API
        // Refund thực hiện manual trên PayOS dashboard
        log.info("PayOS refund: txn={}, amount={} (manual refund required)",
                request.gatewayTxnId(), request.amount());

        String refundTxnId = "PAYOS-RF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new RefundResult(true, refundTxnId, "{\"manual\":true,\"note\":\"Refund via PayOS dashboard\"}");
    }

    @Override
    public GatewayPaymentStatus getPaymentStatus(String gatewayTxnId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);

            ResponseEntity<String> response = restTemplate.exchange(
                    PAYOS_API + "/" + gatewayTxnId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            JsonNode json = objectMapper.readTree(response.getBody());
            String code = json.path("code").asText();
            if (!"00".equals(code)) {
                return GatewayPaymentStatus.NOT_FOUND;
            }

            String status = json.path("data").path("status").asText("").toUpperCase(Locale.ROOT);
            return switch (status) {
                case "PAID" -> GatewayPaymentStatus.PAID;
                case "PENDING" -> GatewayPaymentStatus.PENDING;
                case "PROCESSING" -> GatewayPaymentStatus.PROCESSING;
                case "CANCELLED" -> GatewayPaymentStatus.CANCELLED;
                case "EXPIRED" -> GatewayPaymentStatus.EXPIRED;
                case "FAILED" -> GatewayPaymentStatus.FAILED;
                default -> GatewayPaymentStatus.UNKNOWN;
            };
        } catch (HttpClientErrorException.NotFound e) {
            return GatewayPaymentStatus.NOT_FOUND;
        } catch (Exception e) {
            log.warn("PayOS status check failed for {}: {}", gatewayTxnId, e.getMessage());
            return GatewayPaymentStatus.UNKNOWN;
        }
    }

    @Override
    public void cancelPayment(String gatewayTxnId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);

            String url = PAYOS_API + "/" + gatewayTxnId + "/cancel";

            Map<String, String> body = Map.of("cancellationReason", "User cancelled or expired");
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            log.info("PayOS cancel: orderCode={}", gatewayTxnId);

        } catch (Exception e) {
            log.warn("PayOS cancel failed (best effort): {}", e.getMessage());
        }
    }

    private String appendQuery(String baseUrl, String name, String value) {
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + name + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * HMAC SHA256 signature
     */
    private String hmacSHA256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (Exception e) {
            throw new RuntimeException("HMAC SHA256 error", e);
        }
    }

    public boolean checkPaymentStatus(String orderCode) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);

            ResponseEntity<String> response = restTemplate.exchange(
                    PAYOS_API + "/" + orderCode, HttpMethod.GET,
                    new HttpEntity<>(headers), String.class);

            JsonNode json = objectMapper.readTree(response.getBody());
            String status = json.get("data").get("status").asText();

            return "PAID".equals(status);
        } catch (Exception e) {
            log.warn("PayOS check status failed: {}", e.getMessage());
            return false;
        }
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
