package com.booking.camunda.infrastructure.http;

import com.booking.camunda.application.port.out.MailNotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@Slf4j
public class AuthMailNotificationAdapter implements MailNotificationPort {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.service.auth-url}")
    private String authServiceUrl;

    @Value("${app.internal.api-key}")
    private String internalApiKey;

    @Override
    public void sendHotelDecisionEmail(String to, String hotelName, String decision, String comment) {
        String url = authServiceUrl + "/api/internal/emails/hotel-decision";
        HttpHeaders headers = buildHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of(
                "to", to,
                "hotelName", hotelName != null ? hotelName : "Hotel",
                "decision", decision,
                "comment", comment != null ? comment : ""
        );

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );
            log.info("Hotel decision email response: status={}, body={}",
                    response.getStatusCode(), response.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send hotel decision email: " + e.getMessage(), e);
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalApiKey);
        return headers;
    }
}
