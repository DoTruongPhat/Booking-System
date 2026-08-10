package com.booking.camunda.infrastructure.camunda.worker.hotel;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("recordApprovalAuditDelegate")
@Slf4j
public class RecordApprovalAuditWorker implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        String hotelId = (String) execution.getVariable("hotelId");
        String reviewerId = (String) execution.getVariable("reviewerId");

        execution.setVariable("reviewStatus", "APPROVED");
        log.info("Hotel approval audit recorded: hotelId={}, reviewerId={}", hotelId, reviewerId);
    }
}
