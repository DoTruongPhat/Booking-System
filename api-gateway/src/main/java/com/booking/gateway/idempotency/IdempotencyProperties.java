package com.booking.gateway.idempotency;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Component
@ConfigurationProperties(prefix = "idempotency")
public class IdempotencyProperties {

    private boolean enabled = true;
    private Duration lockTtl = Duration.ofSeconds(120);
    private Duration cacheTtl = Duration.ofHours(24);
    private long maxBodySizeBytes = 1024 * 1024;
    private String headerName = "Idempotency-Key";
    private String lockPrefix = "idempotency:lock:";
    private String cachePrefix = "idempotency:resp:";
    private List<String> endpoints = List.of(
            "POST:/api/user/bookings",
            "POST:/api/user/bookings/*/cancel",
            "POST:/api/user/payments/init",
            "POST:/api/admin/payments/*/refund",
            "POST:/api/host/hotels",
            "POST:/api/host/hotels/*/rooms"
    );

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getLockTtl() {
        return lockTtl;
    }

    public void setLockTtl(Duration lockTtl) {
        this.lockTtl = lockTtl;
    }

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }

    public long getMaxBodySizeBytes() {
        return maxBodySizeBytes;
    }

    public void setMaxBodySizeBytes(long maxBodySizeBytes) {
        this.maxBodySizeBytes = maxBodySizeBytes;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getLockPrefix() {
        return lockPrefix;
    }

    public void setLockPrefix(String lockPrefix) {
        this.lockPrefix = lockPrefix;
    }

    public String getCachePrefix() {
        return cachePrefix;
    }

    public void setCachePrefix(String cachePrefix) {
        this.cachePrefix = cachePrefix;
    }

    public List<String> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<String> endpoints) {
        this.endpoints = endpoints;
    }

    public String buildLockKey(String idempotencyKey) {
        return lockPrefix + idempotencyKey;
    }

    public String buildCacheKey(String userId, String idempotencyKey) {
        return cachePrefix + userId + ":" + idempotencyKey;
    }

    public String matchingEndpoint(String method, String path) {
        if (!enabled) {
            return null;
        }

        String normalizedMethod = method.toUpperCase(Locale.ROOT);
        return endpoints.stream()
                .filter(rule -> matchesRule(rule, normalizedMethod, path))
                .findFirst()
                .orElse(null);
    }

    public boolean isApplicable(String method, String path) {
        return matchingEndpoint(method, path) != null;
    }

    private boolean matchesRule(String rule, String method, String path) {
        int separatorIndex = rule.indexOf(':');
        if (separatorIndex <= 0 || separatorIndex == rule.length() - 1) {
            return false;
        }

        String ruleMethod = rule.substring(0, separatorIndex).toUpperCase(Locale.ROOT);
        String rulePath = rule.substring(separatorIndex + 1);
        return ruleMethod.equals(method) && matchesPath(rulePath, path);
    }

    private boolean matchesPath(String rulePath, String actualPath) {
        String[] ruleSegments = trimSlashes(rulePath).split("/");
        String[] pathSegments = trimSlashes(actualPath).split("/");
        if (ruleSegments.length != pathSegments.length) {
            return false;
        }

        for (int i = 0; i < ruleSegments.length; i++) {
            if (!"*".equals(ruleSegments[i]) && !ruleSegments[i].equals(pathSegments[i])) {
                return false;
            }
        }
        return true;
    }

    private String trimSlashes(String value) {
        String result = value == null ? "" : value.trim();
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
