package com.booking.camunda.infrastructure.camunda.worker.hotel;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component("prepareAdminReviewDelegate")
@Slf4j
public class PrepareAdminReviewWorker implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        String hotelId = (String) execution.getVariable("hotelId");
        String hotelName = (String) execution.getVariable("hotelName");

        execution.setVariable("reviewStatus", "WAITING_ADMIN_REVIEW");
        execution.setVariable("reviewOpenedAt", Instant.now().toString());
        execution.setVariable("reviewReminderCount", 0);

        log.info("Hotel review prepared: hotelId={}, hotelName={}", hotelId, hotelName);
    }
}
