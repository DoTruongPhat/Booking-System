package com.booking.camunda.infrastructure.messaging;

import com.booking.camunda.application.port.in.StartHotelApprovalWorkflowUseCase;
import com.booking.camunda.domain.event.HotelChangeRequestedEvent;
import com.booking.camunda.domain.event.HotelCreatedEvent;
import com.booking.camunda.domain.model.HotelApprovalContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CoreEventConsumer {

    private final ObjectMapper objectMapper;
    private final StartHotelApprovalWorkflowUseCase startHotelApprovalWorkflowUseCase;

    @KafkaListener(
            topics = "${app.kafka.topic.core-events:core-events}",
            groupId = "${spring.kafka.consumer.group-id:camunda-workflow-group}"
    )
    public void consume(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            if (isHotelChangeRequestedEvent(node)) {
                HotelChangeRequestedEvent event = objectMapper.treeToValue(node, HotelChangeRequestedEvent.class);
                startHotelApprovalWorkflowUseCase.start(new HotelApprovalContext(
                        event.hotelId(),
                        event.changeRequestId(),
                        event.ownerUserId(),
                        event.name(),
                        event.city(),
                        event.hostEmail(),
                        "UPDATE_HOTEL",
                        event.proposedChanges()
                ));
                return;
            }

            if (isHotelCreatedEvent(node)) {
                HotelCreatedEvent event = objectMapper.treeToValue(node, HotelCreatedEvent.class);
                startHotelApprovalWorkflowUseCase.startFromEvent(event);
            }
        } catch (Exception e) {
            log.error("Failed to handle core event for workflow: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to handle core event", e);
        }
    }

    private boolean isHotelCreatedEvent(JsonNode node) {
        return node.hasNonNull("hotelId")
                && node.hasNonNull("ownerUserId")
                && node.hasNonNull("name");
    }

    private boolean isHotelChangeRequestedEvent(JsonNode node) {
        return node.hasNonNull("changeRequestId")
                && node.hasNonNull("hotelId")
                && node.hasNonNull("ownerUserId");
    }
}
