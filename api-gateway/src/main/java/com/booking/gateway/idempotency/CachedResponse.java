package com.booking.gateway.idempotency;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Map;

/**
 * DTO được serialize vào Redis để cache response.
 * Lưu đủ thông tin để reconstruct response gốc về cho client.
 */
public class CachedResponse implements Serializable {

    private final int statusCode;

    /**
     * Headers cần trả lại cho client.
     * Chỉ lưu safe headers, bỏ qua hop-by-hop headers (Transfer-Encoding, Connection...).
     */
    private final Map<String, String> headers;

    /** Response body dạng String (JSON thường là UTF-8) */
    private final String body;

    /** Thời điểm cache được tạo (epoch millis) — dùng để debug */
    private final long cachedAt;

    @JsonCreator
    public CachedResponse(
            @JsonProperty("statusCode") int statusCode,
            @JsonProperty("headers") Map<String, String> headers,
            @JsonProperty("body") String body,
            @JsonProperty("cachedAt") long cachedAt) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
        this.cachedAt = cachedAt;
    }

    public static CachedResponse of(int statusCode, Map<String, String> headers, String body) {
        return new CachedResponse(statusCode, headers, body, System.currentTimeMillis());
    }

    public int getStatusCode() { return statusCode; }
    public Map<String, String> getHeaders() { return headers; }
    public String getBody() { return body; }
    public long getCachedAt() { return cachedAt; }

    @Override
    public String toString() {
        return "CachedResponse{status=" + statusCode + ", cachedAt=" + cachedAt + "}";
    }
}