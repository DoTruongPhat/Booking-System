package com.booking.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        long startTime = System.currentTimeMillis();
        String path = request.getPath().value();
        String targetService = resolveService(path);

        log.info("[GW-{}] >> {} {} >> [{}] from {}",
                traceId,
                request.getMethod(),
                path,
                targetService,
                request.getRemoteAddress());

        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-Trace-Id", traceId)
                .header("X-Gateway-Time", String.valueOf(startTime))
                .build();

        return chain.filter(exchange.mutate()
                        .request(mutatedRequest)
                        .build())
                .then(Mono.fromRunnable(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    int statusCode = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value()
                            : 0;

                    if (statusCode >= 400) {
                        log.warn("[GW-{}] << {} {} >> [{}] | {} | {}ms",
                                traceId, request.getMethod(), path,
                                targetService, statusCode, duration);
                    } else {
                        log.info("[GW-{}] << {} {} >> [{}] | {} | {}ms",
                                traceId, request.getMethod(), path,
                                targetService, statusCode, duration);
                    }
                }));
    }

    /**
     * Map request path to target service name for logging.
     * Order matters: more specific paths MUST come before general ones.
     */
    private String resolveService(String path) {
        // Auth Service (8081)
        if (path.startsWith("/api/auth"))            return "AUTH-SERVICE";
        if (path.startsWith("/api/admin/dashboard")) return "AUTH-SERVICE";
        if (path.startsWith("/api/admin/users"))     return "AUTH-SERVICE";
        if (path.startsWith("/api/users"))           return "AUTH-SERVICE";
        if (path.startsWith("/api/internal"))        return "AUTH-SERVICE";

        // Core / Booking Service (8082)
        if (path.startsWith("/api/admin/hotels"))    return "CORE-SERVICE";
        if (path.startsWith("/api/admin/bookings"))  return "CORE-SERVICE";
        if (path.startsWith("/api/admin/room-types")) return "CORE-SERVICE";
        if (path.startsWith("/api/admin/promotions")) return "CORE-SERVICE";
        if (path.startsWith("/api/admin/vouchers"))  return "CORE-SERVICE";
        if (path.startsWith("/api/user/bookings"))   return "CORE-SERVICE";
        if (path.startsWith("/api/host/dashboard"))  return "CORE-SERVICE";
        if (path.startsWith("/api/host"))            return "CORE-SERVICE";
        if (path.startsWith("/api/rooms"))           return "CORE-SERVICE";
        if (path.startsWith("/api/hotels"))          return "CORE-SERVICE";
        if (path.startsWith("/api/vouchers"))        return "CORE-SERVICE";
        if (path.startsWith("/api/bookings"))        return "CORE-SERVICE";

        // Payment Service (8083)
        if (path.startsWith("/api/payments"))        return "PAYMENT-SERVICE";

        // Gateway internal
        if (path.startsWith("/api/composite"))       return "GATEWAY-COMPOSITE";
        if (path.startsWith("/actuator"))            return "GATEWAY";

        return "UNKNOWN";
    }

    @Override
    public int getOrder() {
        return -3;
    }
}
