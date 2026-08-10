package com.booking.camunda.infrastructure.camunda.worker.hotel;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("recordRejectionAuditDelegate")
@Slf4j
public class RecordRejectionAuditWorker implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        String hotelId = (String) execution.getVariable("hotelId");
        String reviewerId = (String) execution.getVariable("reviewerId");
        String comment = (String) execution.getVariable("comment");

        execution.setVariable("reviewStatus", "REJECTED");
        execution.setVariable("rejectionReason", comment);
        log.info("Hotel rejection audit recorded: hotelId={}, reviewerId={}, reason={}",
                hotelId, reviewerId, comment);
    }
}
