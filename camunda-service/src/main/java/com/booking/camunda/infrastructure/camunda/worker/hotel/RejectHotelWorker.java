package com.booking.camunda.infrastructure.camunda.worker.hotel;

import com.booking.camunda.application.port.out.HotelCommandPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("rejectHotelDelegate")
@RequiredArgsConstructor
@Slf4j
public class RejectHotelWorker implements JavaDelegate {

    private final HotelCommandPort hotelCommandPort;

    @Override
    public void execute(DelegateExecution execution) {
        String hotelId = (String) execution.getVariable("hotelId");
        String hotelName = (String) execution.getVariable("hotelName");
        String comment = (String) execution.getVariable("comment");
        String businessKey = execution.getBusinessKey();
        String workflowType = (String) execution.getVariable("workflowType");
        String reviewerId = (String) execution.getVariable("reviewerId");

        log.info("Reject hotel worker started: hotelId={}, hotelName={}", hotelId, hotelName);
        hotelCommandPort.rejectWorkflow(businessKey, workflowType, reviewerId, comment);

        execution.setVariable("approvalResult", "REJECTED");
        execution.setVariable("resultMessage", "Hotel has been rejected: " + comment);

        log.info("Reject hotel worker completed: hotelId={}", hotelId);
    }
}
