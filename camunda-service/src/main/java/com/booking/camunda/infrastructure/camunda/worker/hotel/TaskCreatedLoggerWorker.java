package com.booking.camunda.infrastructure.camunda.worker.hotel;

import com.booking.camunda.application.port.out.HotelCommandPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

@Component("taskCreatedLogger")
@RequiredArgsConstructor
@Slf4j
public class TaskCreatedLoggerWorker implements TaskListener {

    private final HotelCommandPort hotelCommandPort;

    @Override
    public void notify(DelegateTask delegateTask) {
        String hotelId = (String) delegateTask.getVariable("hotelId");
        String businessKey = delegateTask.getExecution().getBusinessKey();

        hotelCommandPort.markTaskCreated(
                hotelId,
                businessKey,
                delegateTask.getId(),
                delegateTask.getName()
        );

        log.info("Workflow task created: taskId={}, taskName={}, businessKey={}",
                delegateTask.getId(),
                delegateTask.getName(),
                delegateTask.getExecution().getBusinessKey());
    }
}
