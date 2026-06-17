package com.booking.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * JwtAuthFilter = verify JWT tại Gateway
 *
 * Tại sao implement GlobalFilter?
 * → GlobalFilter chạy cho TẤT CẢ requests qua Gateway
 * → Không cần config từng route
 *
 * Tại sao implement Ordered?
 * → Định nghĩa thứ tự chạy của filter
 * → getOrder() = -1 → chạy trước các filter khác
 *
 * Luồng:
 * Request đến Gateway
 * → JwtAuthFilter check path có public không
 * → Public path → skip, cho đi tiếp
 * → Protected path → check Authorization header
 * → Không có token → 401
 * → Có token → forward đến service
 * → Service tự verify JWT chi tiết hơn nếu cần
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    // Danh sách public paths
    // → Không cần JWT để gọi các endpoint này
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/2fa/verify",
            "/api/internal",
            "/api/composite",
            "/actuator"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {

        String path = exchange.getRequest().getPath().value();

        // Bước 1: Check path có public không
        // → Public path → skip filter, cho đi tiếp
        // → anyMatch: kiểm tra path có bắt đầu bằng
        //   bất kỳ public path nào không
        boolean isPublic = PUBLIC_PATHS.stream()
                .anyMatch(path::startsWith);

        if (isPublic) {
            // Public endpoint → không cần JWT
            // → Cho request đi tiếp vào service
            return chain.filter(exchange);
        }

        // Bước 2: Lấy Authorization header
        // → Format: "Bearer eyJhbGci..."
        // → Không có header → 401
        String token = extractToken(exchange);

        if (token == null || token.isBlank()) {
            // Không có token → trả về 401 Unauthorized
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Bước 3: Token có format đúng → cho đi tiếp
        // → Service sẽ verify JWT chi tiết hơn
        //   (check signature, blacklist, expiry...)
        // → Gateway chỉ check format cơ bản
        //   để tránh forward request rõ ràng invalid
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // -1 = chạy trước các filter khác của Gateway
        // → Đảm bảo check auth trước khi routing
        return -1;
    }

    private String extractToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        HttpCookie cookie = exchange.getRequest()
                .getCookies()
                .getFirst("access_token");

        if (cookie != null && cookie.getValue() != null && !cookie.getValue().isBlank()) {
            return cookie.getValue();
        }
        return null;
    }
}