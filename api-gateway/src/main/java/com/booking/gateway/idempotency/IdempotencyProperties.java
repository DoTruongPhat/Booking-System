package com.booking.gateway.idempotency;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "idempotency")
public class IdempotencyProperties {

    /** TTL cho lock key (thời gian xử lý tối đa 1 request) */
    private Duration lockTtl = Duration.ofSeconds(30);

    /** TTL cho cached response (window idempotency) */
    private Duration cacheTtl = Duration.ofHours(24);

    /** Các HTTP method cần áp dụng idempotency */
    private List<String> methods = List.of("POST", "PUT", "PATCH", "DELETE");

    /** Các path prefix cần áp dụng idempotency */
    private List<String> includedPaths = List.of("/api/bookings", "/api/payments");

    /** Tên header client gửi lên */
    private String headerName = "Idempotency-Key";

    /** Redis key prefix cho lock */
    private String lockPrefix = "idempotency:lock:";

    /** Redis key prefix cho cached response */
    private String cachePrefix = "idempotency:";

    // ──────────────────────────────────────────────
    // Getters & Setters
    // ──────────────────────────────────────────────

    public Duration getLockTtl() { return lockTtl; }
    public void setLockTtl(Duration lockTtl) { this.lockTtl = lockTtl; }

    public Duration getCacheTtl() { return cacheTtl; }
    public void setCacheTtl(Duration cacheTtl) { this.cacheTtl = cacheTtl; }

    public List<String> getMethods() { return methods; }
    public void setMethods(List<String> methods) { this.methods = methods; }

    public List<String> getIncludedPaths() { return includedPaths; }
    public void setIncludedPaths(List<String> includedPaths) { this.includedPaths = includedPaths; }

    public String getHeaderName() { return headerName; }
    public void setHeaderName(String headerName) { this.headerName = headerName; }

    public String getLockPrefix() { return lockPrefix; }
    public void setLockPrefix(String lockPrefix) { this.lockPrefix = lockPrefix; }

    public String getCachePrefix() { return cachePrefix; }
    public void setCachePrefix(String cachePrefix) { this.cachePrefix = cachePrefix; }

    // ──────────────────────────────────────────────
    // Helper methods
    // ──────────────────────────────────────────────

    /**
     * idempotency:lock:{idempotencyKey}
     */
    public String buildLockKey(String idempotencyKey) {
        return lockPrefix + idempotencyKey;
    }

    /**
     * idempotency:{userId}:{idempotencyKey}
     */
    public String buildCacheKey(String userId, String idempotencyKey) {
        return cachePrefix + userId + ":" + idempotencyKey;
    }

    public boolean isMethodApplicable(String method) {
        return methods.contains(method.toUpperCase());
    }

    public boolean isPathApplicable(String path) {
        return includedPaths.stream().anyMatch(path::startsWith);
    }
}