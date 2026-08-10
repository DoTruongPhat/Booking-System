package com.booking.gateway.idempotency;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class IdempotencyFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyFilter.class);
    private static final int FILTER_ORDER = 1;
    private static final int RETRY_AFTER_SECONDS = 5;
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String REPLAY_HEADER = "X-Idempotent-Replay";
    private static final String ORIGINAL_TRACE_HEADER = "X-Original-Trace-Id";

    private static final Set<String> EXCLUDED_HEADERS = Set.of(
            "transfer-encoding",
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailers",
            "upgrade",
            "content-length"
    );

    private final IdempotencyRedisService redisService;
    private final IdempotencyProperties props;
    private final MeterRegistry meterRegistry;

    public IdempotencyFilter(
            IdempotencyRedisService redisService,
            IdempotencyProperties props,
            MeterRegistry meterRegistry) {
        this.redisService = redisService;
        this.props = props;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public int getOrder() {
        return FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Timer.Sample sample = Timer.start(meterRegistry);
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod().name();
        String path = request.getPath().value();
        String endpoint = props.matchingEndpoint(method, path);

        if (endpoint == null) {
            return chain.filter(exchange);
        }

        String idempotencyKey = request.getHeaders().getFirst(props.getHeaderName());
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return respondError(
                    exchange,
                    HttpStatus.BAD_REQUEST,
                    "IDEMPOTENCY_001",
                    "Missing required header: " + props.getHeaderName(),
                    null
            );
        }

        if (!isValidUUID(idempotencyKey)) {
            return respondError(
                    exchange,
                    HttpStatus.BAD_REQUEST,
                    "IDEMPOTENCY_001",
                    "Idempotency-Key must be a valid UUID",
                    null
            );
        }

        String userId = request.getHeaders().getFirst(USER_ID_HEADER);
        if (userId == null || userId.isBlank()) {
            return respondError(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_001",
                    "Authenticated user is required for idempotent requests",
                    null
            );
        }

        return redisService.getCachedResponse(userId, idempotencyKey)
                .flatMap(cached -> {
                    counter("idempotency.cache.hit", endpoint);
                    counter("idempotency_cache_hit_total", endpoint);
                    log.info("Idempotency cache hit. key={}, userId={}, endpoint={}", idempotencyKey, userId, endpoint);
                    return writeCachedResponse(exchange, cached);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    counter("idempotency.cache.miss", endpoint);
                    counter("idempotency_cache_miss_total", endpoint);
                    return processNewRequest(exchange, chain, userId, idempotencyKey, endpoint);
                }))
                .doFinally(signalType -> recordDuration(sample, endpoint, signalType.name()));
    }

    private Mono<Void> processNewRequest(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            String userId,
            String idempotencyKey,
            String endpoint) {

        return redisService.acquireLock(idempotencyKey, userId)
                .flatMap(acquired -> {
                    if (!acquired) {
                        counter("idempotency.lock.conflict", endpoint);
                        counter("idempotency_lock_conflict_total", endpoint);
                        log.info("Idempotency lock conflict. key={}, userId={}, endpoint={}", idempotencyKey, userId, endpoint);
                        return respondError(
                                exchange,
                                HttpStatus.CONFLICT,
                                "IDEMPOTENCY_002",
                                "Request in progress, please retry",
                                RETRY_AFTER_SECONDS
                        );
                    }

                    CapturingResponseDecorator decorator =
                            new CapturingResponseDecorator(exchange.getResponse(), props.getMaxBodySizeBytes());
                    ServerWebExchange mutatedExchange = exchange.mutate().response(decorator).build();
                    String originalTraceId = resolveTraceId(exchange.getRequest());

                    Mono<Void> downstream = chain.filter(mutatedExchange)
                            .then(Mono.defer(() -> cacheIfCacheable(
                                    decorator,
                                    userId,
                                    idempotencyKey,
                                    endpoint,
                                    originalTraceId
                            )));

                    return downstream
                            .then(redisService.releaseLock(idempotencyKey).then())
                            .onErrorResume(error -> redisService.releaseLock(idempotencyKey)
                                    .then(Mono.error(error)));
                });
    }

    private Mono<Void> cacheIfCacheable(
            CapturingResponseDecorator decorator,
            String userId,
            String idempotencyKey,
            String endpoint,
            String originalTraceId) {

        int status = decorator.getStatusCode() != null ? decorator.getStatusCode().value() : 200;

        if (isStreamingResponse(decorator.getHeaders())) {
            counter("idempotency.cache.skipped", endpoint, "reason", "streaming");
            counter("idempotency_cache_skipped_total", endpoint, "reason", "streaming");
            log.debug("Skipping idempotency cache for streaming response. key={}, status={}", idempotencyKey, status);
            return Mono.empty();
        }

        if (decorator.isBodyTooLarge()) {
            counter("idempotency.cache.skipped", endpoint, "reason", "body_too_large");
            counter("idempotency_cache_skipped_total", endpoint, "reason", "body_too_large");
            log.debug("Skipping idempotency cache because body is too large. key={}, status={}", idempotencyKey, status);
            return Mono.empty();
        }

        if (!isCacheableStatus(status)) {
            counter("idempotency.cache.skipped", endpoint, "reason", "status");
            counter("idempotency_cache_skipped_total", endpoint, "reason", "status");
            log.debug("Skipping idempotency cache for status={}. key={}", status, idempotencyKey);
            return Mono.empty();
        }

        Map<String, String> headers = extractSafeHeaders(decorator.getHeaders());
        CachedResponse cached = CachedResponse.of(status, headers, decorator.getCapturedBody(), originalTraceId);
        return redisService.cacheResponse(userId, idempotencyKey, cached);
    }

    private Mono<Void> writeCachedResponse(ServerWebExchange exchange, CachedResponse cached) {
        ServerHttpResponse response = exchange.getResponse();
        HttpStatus status = HttpStatus.resolve(cached.getStatus());
        response.setStatusCode(status != null ? status : HttpStatus.OK);

        if (!response.isCommitted()) {
            cached.getHeaders().forEach((name, value) -> response.getHeaders().set(name, value));
            response.getHeaders().set(REPLAY_HEADER, "true");
            if (cached.getOriginalTraceId() != null && !cached.getOriginalTraceId().isBlank()) {
                response.getHeaders().set(ORIGINAL_TRACE_HEADER, cached.getOriginalTraceId());
            }
        }

        DataBuffer buffer = response.bufferFactory().wrap(cached.bodyBytes());
        return response.writeWith(Mono.just(buffer));
    }

    private boolean isCacheableStatus(int status) {
        return (status >= 200 && status < 300) || (status >= 400 && status < 500 && status != 409);
    }

    private boolean isStreamingResponse(HttpHeaders headers) {
        MediaType contentType = headers.getContentType();
        if (contentType != null) {
            String value = contentType.toString().toLowerCase(Locale.ROOT);
            if (value.contains("text/event-stream") || value.contains("application/stream+json")) {
                return true;
            }
        }

        return false;
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
            String lowerName = name.toLowerCase(Locale.ROOT);
            if (!EXCLUDED_HEADERS.contains(lowerName) && !values.isEmpty()) {
                safe.put(name, values.get(0));
            }
        });
        return safe;
    }

    private String resolveTraceId(ServerHttpRequest request) {
        String traceId = request.getHeaders().getFirst("X-Trace-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = request.getHeaders().getFirst("X-Request-Id");
        }
        return traceId == null || traceId.isBlank() ? request.getId() : traceId;
    }

    private Mono<Void> respondError(
            ServerWebExchange exchange,
            HttpStatus status,
            String errorCode,
            String message,
            Integer retryAfter) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        if (retryAfter != null) {
            response.getHeaders().set(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
        }

        String retryAfterField = retryAfter == null ? "" : ",\"retryAfter\":" + retryAfter;
        String body = """
                {"success":false,"errorCode":"%s","message":"%s"%s}
                """.formatted(errorCode, escapeJson(message), retryAfterField).strip();

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void counter(String name, String endpoint, String... tags) {
        String[] meterTags = new String[tags.length + 2];
        meterTags[0] = "endpoint";
        meterTags[1] = endpoint;
        System.arraycopy(tags, 0, meterTags, 2, tags.length);
        meterRegistry.counter(name, meterTags).increment();
    }

    private void recordDuration(Timer.Sample sample, String endpoint, String signal) {
        long nanos = sample.stop(meterRegistry.timer(
                "idempotency.filter.duration",
                "endpoint", endpoint,
                "signal", signal
        ));
        meterRegistry.timer(
                "idempotency_filter_duration_seconds",
                "endpoint", endpoint,
                "signal", signal
        ).record(nanos, java.util.concurrent.TimeUnit.NANOSECONDS);
    }

    private static class CapturingResponseDecorator extends ServerHttpResponseDecorator {

        private final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();
        private final ByteArrayOutputStream capturedBody = new ByteArrayOutputStream();
        private final long maxBodySizeBytes;
        private boolean bodyTooLarge;

        CapturingResponseDecorator(ServerHttpResponse delegate, long maxBodySizeBytes) {
            super(delegate);
            this.maxBodySizeBytes = maxBodySizeBytes;
        }

        @Override
        public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
            Flux<DataBuffer> bufferedBody = Flux.from(body)
                    .map(buffer -> {
                        byte[] bytes = new byte[buffer.readableByteCount()];
                        buffer.read(bytes);
                        DataBufferUtils.release(buffer);

                        capture(bytes);
                        return bufferFactory.wrap(bytes);
                    });

            return super.writeWith(bufferedBody);
        }

        @Override
        public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
            return writeWith(Flux.from(body).flatMapSequential(publisher -> publisher));
        }

        byte[] getCapturedBody() {
            return capturedBody.toByteArray();
        }

        boolean isBodyTooLarge() {
            return bodyTooLarge;
        }

        private void capture(byte[] bytes) {
            if (bodyTooLarge) {
                return;
            }

            long nextSize = (long) capturedBody.size() + bytes.length;
            if (nextSize > maxBodySizeBytes) {
                bodyTooLarge = true;
                capturedBody.reset();
                return;
            }

            capturedBody.writeBytes(bytes);
        }
    }
}
