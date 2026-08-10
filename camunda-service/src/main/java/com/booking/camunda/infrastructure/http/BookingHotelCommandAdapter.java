package com.booking.camunda.infrastructure.http;

import com.booking.camunda.application.port.out.HotelCommandPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@Slf4j
public class BookingHotelCommandAdapter implements HotelCommandPort {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.service.booking-url}")
    private String bookingServiceUrl;

    @Value("${app.internal.api-key}")
    private String internalApiKey;

    @Override
    public void markWorkflowStarted(
            String hotelId,
            String businessKey,
            String processInstanceId,
            String processDefinitionId,
            String workflowType
    ) {
        String url = bookingServiceUrl + "/internal/hotels/" + hotelId + "/workflow-started";
        Map<String, String> body = Map.of(
                "businessKey", businessKey,
                "processInstanceId", processInstanceId,
                "processDefinitionId", processDefinitionId != null ? processDefinitionId : "",
                "workflowType", workflowType
        );
        exchange(url, body, "sync workflow started");
    }

    @Override
    public void markTaskCreated(String hotelId, String businessKey, String taskId, String taskName) {
        String url = bookingServiceUrl + "/internal/hotels/" + hotelId + "/workflow-task-created";
        Map<String, String> body = Map.of(
                "businessKey", businessKey,
                "taskId", taskId,
                "taskName", taskName
        );
        exchange(url, body, "sync workflow task created");
    }

    @Override
    public void markTaskAssigned(String taskId, String assignee) {
        String url = bookingServiceUrl + "/internal/hotels/workflow-tasks/" + taskId + "/assignment";
        Map<String, String> body = assignee == null || assignee.isBlank()
                ? Map.of("taskId", taskId)
                : Map.of("taskId", taskId, "assignee", assignee);
        exchange(url, body, "sync workflow task assignment");
    }

    @Override
    public void markDecisionStarted(String businessKey, String workflowType, String decision, String reviewerId, String comment) {
        String url = bookingServiceUrl + "/internal/hotels/workflows/" + businessKey + "/decision-started";
        exchange(url, decisionBody(businessKey, workflowType, decision, reviewerId, comment), "sync workflow decision");
    }

    @Override
    public void approveWorkflow(String businessKey, String workflowType, String reviewerId, String comment) {
        String url = bookingServiceUrl + "/internal/hotels/workflows/" + businessKey + "/approve";
        exchange(url, decisionBody(businessKey, workflowType, "APPROVED", reviewerId, comment), "approve workflow");
    }

    @Override
    public void rejectWorkflow(String businessKey, String workflowType, String reviewerId, String reason) {
        String url = bookingServiceUrl + "/internal/hotels/workflows/" + businessKey + "/reject";
        exchange(url, decisionBody(businessKey, workflowType, "REJECTED", reviewerId, reason), "reject workflow");
    }

    @Override
    public void markIncident(String businessKey, String message) {
        String url = bookingServiceUrl + "/internal/hotels/workflows/" + businessKey + "/incident";
        exchange(url, Map.of("businessKey", businessKey, "message", message), "sync workflow incident");
    }

    private Map<String, String> decisionBody(
            String businessKey,
            String workflowType,
            String decision,
            String reviewerId,
            String comment
    ) {
        return Map.of(
                "businessKey", businessKey,
                "workflowType", workflowType != null ? workflowType : "CREATE_HOTEL",
                "decision", decision,
                "reviewerId", reviewerId != null ? reviewerId : "SYSTEM",
                "comment", comment != null ? comment : ""
        );
    }

    private void exchange(String url, Map<String, String> body, String action) {
        HttpHeaders headers = buildHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);
            log.info("{} response: status={}, body={}", action, response.getStatusCode(), response.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to " + action + ": " + e.getMessage(), e);
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalApiKey);
        return headers;
    }
}
