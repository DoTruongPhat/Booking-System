package com.booking.gateway.composite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class BookingCompositeHandler {

    private static final Logger log = LoggerFactory.getLogger(BookingCompositeHandler.class);

    @Value("${AUTH_SERVICE_URL:http://localhost:8081}")
    private String authServiceUrl;

    @Bean
    public RouterFunction<ServerResponse> compositeRoutes() {
        return RouterFunctions.route()
                .GET("/api/composite/dashboard", this::getDashboard)
                .GET("/api/composite/bookings/{id}/detail", this::getBookingDetail)
                .build();
    }

    @SuppressWarnings("unchecked")
    private Mono<ServerResponse> getDashboard(ServerRequest request) {
        log.info("[Composite] Dashboard request");

        String cookie = request.headers().firstHeader(HttpHeaders.COOKIE);
        WebClient client = WebClient.create();

        Mono<Map> userStats = client.get()
                .uri(authServiceUrl + "/api/admin/dashboard")
                .header(HttpHeaders.COOKIE, cookie != null ? cookie : "")
                .header("X-API-KEY", "dev-api-key-abc123")
                .retrieve()
                .bodyToMono(Map.class);

        Mono<Map> bookingStats = Mono.just(Map.of(
                "totalBookings", 0,
                "message", "Booking Service not available yet"));

        Mono<Map> paymentStats = Mono.just(Map.of(
                "totalPayments", 0,
                "message", "Payment Service not available yet"));

        return Mono.zip(userStats, bookingStats, paymentStats)
                .flatMap(tuple -> {
                    Map<String, Object> dashboard = new HashMap<>();
                    dashboard.put("users", tuple.getT1());
                    dashboard.put("bookings", tuple.getT2());
                    dashboard.put("payments", tuple.getT3());
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(dashboard);
                });
    }

    private Mono<ServerResponse> getBookingDetail(ServerRequest request) {
        String id = request.pathVariable("id");
        log.info("[Composite] Booking detail for: {}", id);

        Map<String, Object> response = new HashMap<>();
        response.put("bookingId", id);
        response.put("message", "Booking + Payment Service not available yet");
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(response);
    }
}