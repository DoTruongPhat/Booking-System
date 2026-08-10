package com.booking.camunda.infrastructure.camunda.worker.hotel;

import com.booking.camunda.application.port.out.MailNotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("sendHotelApprovedMailDelegate")
@RequiredArgsConstructor
@Slf4j
public class SendHotelApprovedMailWorker implements JavaDelegate {

    private final MailNotificationPort mailNotificationPort;

    @Override
    public void execute(DelegateExecution execution) {
        String hotelId = (String) execution.getVariable("hotelId");
        String hotelName = (String) execution.getVariable("hotelName");
        String hostEmail = (String) execution.getVariable("hostEmail");
        String comment = (String) execution.getVariable("comment");

        if (hostEmail == null || hostEmail.isBlank()) {
            log.warn("No hostEmail found. Skip approved email: hotelId={}", hotelId);
            return;
        }

        mailNotificationPort.sendHotelDecisionEmail(hostEmail, hotelName, "APPROVED", comment);
        log.info("Approved email sent: hotelId={}, to={}", hotelId, hostEmail);
    }
}
