package com.booking.gateway.idempotency;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Serialized to Redis so the gateway can replay the original response for
 * duplicate idempotent requests.
 */
public class CachedResponse implements Serializable {

    private final int statusCode;
    private final Map<String, String> headers;
    private final byte[] bodyBytes;
    private final String originalTraceId;
    private final long cachedAt;

    @JsonCreator
    public CachedResponse(
            @JsonProperty("statusCode") int statusCode,
            @JsonProperty("headers") Map<String, String> headers,
            @JsonProperty("bodyBytes") byte[] bodyBytes,
            @JsonProperty("body") String legacyBody,
            @JsonProperty("originalTraceId") String originalTraceId,
            @JsonProperty("cachedAt") long cachedAt) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.bodyBytes = bodyBytes != null
                ? bodyBytes.clone()
                : (legacyBody != null ? legacyBody.getBytes(StandardCharsets.UTF_8) : new byte[0]);
        this.originalTraceId = originalTraceId;
        this.cachedAt = cachedAt;
    }

    public static CachedResponse of(
            int statusCode,
            Map<String, String> headers,
            byte[] bodyBytes,
            String originalTraceId) {
        return new CachedResponse(statusCode, headers, bodyBytes, null, originalTraceId, System.currentTimeMillis());
    }

    public int getStatus() {
        return statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public byte[] getBodyBytes() {
        return bodyBytes.clone();
    }

    @JsonIgnore
    public byte[] bodyBytes() {
        return getBodyBytes();
    }

    public String getOriginalTraceId() {
        return originalTraceId;
    }

    public long getCachedAt() {
        return cachedAt;
    }

    @Override
    public String toString() {
        return "CachedResponse{status=" + statusCode
                + ", bodyBytes=" + bodyBytes.length
                + ", cachedAt=" + cachedAt
                + '}';
    }
}
