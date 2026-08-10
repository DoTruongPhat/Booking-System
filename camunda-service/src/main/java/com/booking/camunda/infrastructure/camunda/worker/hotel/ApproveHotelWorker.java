package com.booking.camunda.infrastructure.camunda.worker.hotel;

import com.booking.camunda.application.port.out.HotelCommandPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("approveHotelDelegate")
@RequiredArgsConstructor
@Slf4j
public class ApproveHotelWorker implements JavaDelegate {

    private final HotelCommandPort hotelCommandPort;

    @Override
    public void execute(DelegateExecution execution) {
        String hotelId = (String) execution.getVariable("hotelId");
        String hotelName = (String) execution.getVariable("hotelName");
        String businessKey = execution.getBusinessKey();
        String workflowType = (String) execution.getVariable("workflowType");
        String reviewerId = (String) execution.getVariable("reviewerId");
        String comment = (String) execution.getVariable("comment");

        log.info("Approve hotel worker started: hotelId={}, hotelName={}", hotelId, hotelName);
        hotelCommandPort.approveWorkflow(businessKey, workflowType, reviewerId, comment);

        execution.setVariable("approvalResult", "APPROVED");
        execution.setVariable("resultMessage", "Hotel has been approved and is now ACTIVE");

        log.info("Approve hotel worker completed: hotelId={}", hotelId);
    }
}
