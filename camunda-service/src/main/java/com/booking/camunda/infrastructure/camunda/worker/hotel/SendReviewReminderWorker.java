package com.booking.camunda.infrastructure.camunda.worker.hotel;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component("sendReviewReminderDelegate")
@Slf4j
public class SendReviewReminderWorker implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        String hotelId = (String) execution.getVariable("hotelId");
        String hotelName = (String) execution.getVariable("hotelName");
        Object currentCount = execution.getVariable("reviewReminderCount");
        int reminderCount = currentCount instanceof Number number ? number.intValue() : 0;

        execution.setVariable("reviewReminderCount", reminderCount + 1);
        execution.setVariable("lastReviewReminderAt", Instant.now().toString());
        execution.setVariable("reviewStatus", "REMINDER_SENT");
        execution.setVariable("reminderLevel", resolveReminderLevel(reminderCount + 1));
        execution.setVariable(
                "notificationMessage",
                "Hotel approval task is still waiting for admin review"
        );

        log.info("Hotel review reminder triggered: hotelId={}, hotelName={}, count={}",
                hotelId, hotelName, reminderCount + 1);
    }

    private String resolveReminderLevel(int count) {
        if (count >= 3) {
            return "HIGH";
        }
        if (count >= 2) {
            return "MEDIUM";
        }
        return "LOW";
    }
}
