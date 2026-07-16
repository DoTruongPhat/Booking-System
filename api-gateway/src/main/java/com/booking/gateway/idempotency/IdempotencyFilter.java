package com.booking.gateway.idempotency;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class IdempotencyFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyFilter.class);

    /**
     * Order = 1: chạy sau AuthFilter (order=0 thường),
     * nhưng trước các filter downstream khác.
     */
    private static final int FILTER_ORDER = 1;

    /**
     * Hop-by-hop headers không nên cache lại.
     */
    private static final Set<String> EXCLUDED_HEADERS = Set.of(
            "transfer-encoding", "connection", "keep-alive",
            "proxy-authenticate", "proxy-authorization",
            "te", "trailers", "upgrade"
    );

    /**
     * Header gateway đặt sau khi Auth filter decode JWT.
     * Adjust tên này cho đúng với AuthFilter của bạn.
     */
    private static final String USER_ID_HEADER = "X-User-Id";

    private final IdempotencyRedisService redisService;
    private final IdempotencyProperties props;

    public IdempotencyFilter(IdempotencyRedisService redisService, IdempotencyProperties props) {
        this.redisService = redisService;
        this.props = props;
    }

    @Override
    public int getOrder() {
        return FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 1. Skip nếu method hoặc path không applicable
        if (!shouldApply(request)) {
            return chain.filter(exchange);
        }

        // 2. Validate Idempotency-Key header
        String idempotencyKey = request.getHeaders().getFirst(props.getHeaderName());
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return respondError(exchange, HttpStatus.BAD_REQUEST,
                    "Missing required header: " + props.getHeaderName());
        }

        if (!isValidUUID(idempotencyKey)) {
            return respondError(exchange, HttpStatus.BAD_REQUEST,
                    "Idempotency-Key must be a valid UUID");
        }

        // 3. Extract userId từ JWT (đã được AuthFilter decode và đặt vào header)
        String userId = request.getHeaders().getFirst(USER_ID_HEADER);
        if (userId == null || userId.isBlank()) {
            log.warn("Missing {} header — idempotency cache will be skipped", USER_ID_HEADER);
            // Fail-open: vẫn forward request, chỉ không cache
            return chain.filter(exchange);
        }

        // 4. Check cache trước (fast-path: đã có response cached)
        return redisService.getCachedResponse(userId, idempotencyKey)
                .flatMap(cached -> {
                    log.info("Idempotency cache hit — returning cached response. key={}, userId={}",
                            idempotencyKey, userId);
                    return writeCachedResponse(exchange, cached);
                })
                .switchIfEmpty(
                        // 5. Chưa có cache → thử acquire lock
                        processNewRequest(exchange, chain, userId, idempotencyKey)
                );
    }

    /**
     * Xử lý request lần đầu (chưa có cache):
     * - SETNX lock
     * - Forward request với response decorator để capture body
     * - Cache response nếu 2xx
     */
    private Mono<Void> processNewRequest(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            String userId,
            String idempotencyKey) {

        return redisService.acquireLock(idempotencyKey)
                .flatMap(acquired -> {
                    if (!acquired) {
                        // Lock đang bị hold → request đang được xử lý
                        log.info("Idempotency lock conflict. key={}, userId={}", idempotencyKey, userId);
                        return respondError(exchange, HttpStatus.CONFLICT,
                                "Request in progress. Please retry after a moment.");
                    }

                    // Lock acquired → decorate response để capture body
                    CapturingResponseDecorator decorator =
                            new CapturingResponseDecorator(exchange.getResponse());
                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .response(decorator)
                            .build();

                    return chain.filter(mutatedExchange)
                            .then(Mono.defer(() -> cacheIfSuccessful(
                                    decorator, userId, idempotencyKey)))
                            .then(Mono.defer(() ->
                                    redisService.releaseLock(idempotencyKey)))
                            .then();
                });
    }

    /**
     * Cache response nếu status 2xx.
     */
    private Mono<Void> cacheIfSuccessful(
            CapturingResponseDecorator decorator,
            String userId,
            String idempotencyKey) {

        int statusCode = decorator.getStatusCode() != null
                ? decorator.getStatusCode().value()
                : 200;

        if (statusCode >= 200 && statusCode < 300) {
            String body = decorator.getCapturedBody();
            Map<String, String> headers = extractSafeHeaders(decorator.getHeaders());

            CachedResponse cached = CachedResponse.of(statusCode, headers, body);
            return redisService.cacheResponse(userId, idempotencyKey, cached);
        }

        log.debug("Non-2xx response ({}), skipping cache. key={}", statusCode, idempotencyKey);
        return Mono.empty();
    }

    /**
     * Trả về cached response về cho client.
     */
    private Mono<Void> writeCachedResponse(ServerWebExchange exchange, CachedResponse cached) {
        ServerHttpResponse response = exchange.getResponse();

        // Set status
        response.setStatusCode(HttpStatus.resolve(cached.getStatusCode()));

        // Set headers từ cache (bỏ qua nếu response đã committed)
        if (!response.isCommitted()) {
            cached.getHeaders().forEach((k, v) -> response.getHeaders().set(k, v));
            // Đánh dấu đây là cached response để client/debug biết
            response.getHeaders().set("X-Idempotency-Replayed", "true");
        }

        // Write body
        String body = cached.getBody() != null ? cached.getBody() : "";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);

        return response.writeWith(Mono.just(buffer));
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private boolean shouldApply(ServerHttpRequest request) {
        String method = request.getMethod().name();
        String path = request.getPath().value();
        return props.isMethodApplicable(method) && props.isPathApplicable(path);
    }

    private boolean isValidUUID(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private Map<String, String> extractSafeHeaders(HttpHeaders headers) {
        Map<String, String> safe = new HashMap<>();
        headers.forEach((name, values) -> {
            if (!EXCLUDED_HEADERS.contains(name.toLowerCase()) && !values.isEmpty()) {
                safe.put(name, values.get(0));
            }
        });
        return safe;
    }

    private Mono<Void> respondError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {"error":"%s","message":"%s"}
                """.formatted(status.getReasonPhrase(), message).strip();

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    // ──────────────────────────────────────────────
    // Response Decorator: capture body từ downstream
    // ──────────────────────────────────────────────

    /**
     * Decorator để intercept và capture response body.
     * Vừa capture vừa forward body về client như bình thường.
     */
    private static class CapturingResponseDecorator extends ServerHttpResponseDecorator {

        private final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();
        private final StringBuilder capturedBody = new StringBuilder();

        public CapturingResponseDecorator(ServerHttpResponse delegate) {
            super(delegate);
        }

        @Override
        public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
            Flux<DataBuffer> bufferedBody = Flux.from(body)
                    .map(buffer -> {
                        // Đọc bytes để capture
                        byte[] bytes = new byte[buffer.readableByteCount()];
                        buffer.read(bytes);
                        DataBufferUtils.release(buffer);

                        capturedBody.append(new String(bytes, StandardCharsets.UTF_8));

                        // Tạo lại buffer mới để forward về client
                        return bufferFactory.wrap(bytes);
                    });

            return super.writeWith(bufferedBody);
        }

        @Override
        public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
            return writeWith(Flux.from(body).flatMapSequential(p -> p));
        }

        public String getCapturedBody() {
            return capturedBody.toString();
        }
    }
}