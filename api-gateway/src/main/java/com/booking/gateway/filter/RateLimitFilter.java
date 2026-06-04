package com.booking.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RateLimitFilter = Giới hạn request bằng in-memory counter
 *
 * Dùng ConcurrentHashMap thay vì Redis
 * → Không phụ thuộc external service
 * → Gateway hoạt động độc lập
 * → Counter tự reset mỗi phút
 *
 * Order = -2 → sau LoggingFilter, trước JwtAuthFilter
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final int DEFAULT_LIMIT = 100;
    private static final int LOGIN_LIMIT = 5;
    private static final int REGISTER_LIMIT = 3;
    private static final long WINDOW_MS = 60_000; // 1 phút

    // In-memory storage: IP:category → counter
    private final Map<String, RateInfo> rateLimits = new ConcurrentHashMap<>();

    private static final List<String> SKIP_PATHS = List.of("/actuator");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {

        String path = exchange.getRequest().getPath().value();

        if (SKIP_PATHS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        String clientIp = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";

        String category;
        int limit;

        if (path.contains("/auth/login")) {
            category = "login";
            limit = LOGIN_LIMIT;
        } else if (path.contains("/auth/register")) {
            category = "register";
            limit = REGISTER_LIMIT;
        } else {
            category = "general";
            limit = DEFAULT_LIMIT;
        }

        String key = clientIp + ":" + category;

        // Lấy hoặc tạo counter
        RateInfo rateInfo = rateLimits.compute(key, (k, existing) -> {
            long now = System.currentTimeMillis();
            // Reset nếu hết window
            if (existing == null || now - existing.windowStart > WINDOW_MS) {
                return new RateInfo(now, new AtomicInteger(1));
            }
            existing.counter.incrementAndGet();
            return existing;
        });

        int count = rateInfo.counter.get();

        if (count > limit) {
            log.warn("[GW-RateLimit] IP {} exceeded {} limit ({}/{}): {}",
                    clientIp, category, count, limit, path);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -2;
    }

    /**
     * Counter + window start time
     */
    private static class RateInfo {
        final long windowStart;
        final AtomicInteger counter;

        RateInfo(long windowStart, AtomicInteger counter) {
            this.windowStart = windowStart;
            this.counter = counter;
        }
    }
}