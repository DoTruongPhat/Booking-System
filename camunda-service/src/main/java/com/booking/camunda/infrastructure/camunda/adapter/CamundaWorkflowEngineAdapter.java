package com.booking.camunda.infrastructure.camunda.adapter;

import com.booking.camunda.application.port.out.WorkflowEnginePort;
import com.booking.camunda.domain.model.WorkflowInstance;
import com.booking.camunda.domain.model.WorkflowTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CamundaWorkflowEngineAdapter implements WorkflowEnginePort {

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final TaskService taskService;

    @Override
    public WorkflowInstance startProcess(String processKey, String businessKey, Map<String, Object> variables) {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                processKey,
                businessKey,
                variables
        );

        log.info("Workflow started: processKey={}, instanceId={}, businessKey={}",
                processKey, instance.getId(), businessKey);

        return new WorkflowInstance(
                instance.getId(),
                instance.getProcessDefinitionId(),
                businessKey,
                instance.isEnded()
        );
    }

    @Override
    public WorkflowInstance getProcessInstance(String processInstanceId) {
        ProcessInstance active = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (active != null) {
            return toWorkflowInstance(active);
        }

        HistoricProcessInstance historic = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (historic != null) {
            return toWorkflowInstance(historic);
        }

        throw new RuntimeException("Process instance not found: " + processInstanceId);
    }

    @Override
    public WorkflowInstance getProcessByBusinessKey(String businessKey) {
        ProcessInstance active = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .singleResult();

        if (active != null) {
            return toWorkflowInstance(active);
        }

        HistoricProcessInstance historic = historyService.createHistoricProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .orderByProcessInstanceStartTime().desc()
                .list().stream().findFirst().orElse(null);

        if (historic != null) {
            return toWorkflowInstance(historic);
        }

        throw new RuntimeException("Process not found for businessKey: " + businessKey);
    }

    @Override
    public List<WorkflowTask> getTasksByCandidateGroup(String candidateGroup) {
        return taskService.createTaskQuery()
                .taskCandidateGroup(candidateGroup)
                .orderByTaskCreateTime().desc()
                .list()
                .stream()
                .map(this::toWorkflowTask)
                .toList();
    }

    @Override
    public List<WorkflowTask> getTasksByAssignee(String assignee) {
        return taskService.createTaskQuery()
                .taskAssignee(assignee)
                .orderByTaskCreateTime().desc()
                .list()
                .stream()
                .map(this::toWorkflowTask)
                .toList();
    }

    @Override
    public List<WorkflowTask> getAllActiveTasks() {
        return taskService.createTaskQuery()
                .active()
                .orderByTaskCreateTime().desc()
                .list()
                .stream()
                .map(this::toWorkflowTask)
                .toList();
    }

    @Override
    public WorkflowTask getTask(String taskId) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        return toWorkflowTask(task);
    }

    @Override
    public void claimTask(String taskId, String userId) {
        taskService.claim(taskId, userId);
        log.info("Workflow task claimed: taskId={}, userId={}", taskId, userId);
    }

    @Override
    public void unclaimTask(String taskId) {
        taskService.setAssignee(taskId, null);
        log.info("Workflow task unclaimed: taskId={}", taskId);
    }

    @Override
    public void completeTaskAsUser(String taskId, String userId, Map<String, Object> variables) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        if (task.getAssignee() == null) {
            taskService.claim(taskId, userId);
        } else if (!task.getAssignee().equals(userId)) {
            throw new IllegalStateException("Task is assigned to another user");
        }

        taskService.complete(taskId, variables);
        log.info("Workflow task completed: taskId={}, userId={}, taskName={}",
                taskId, userId, task.getName());
    }

    private WorkflowTask toWorkflowTask(Task task) {
        String businessKey = null;
        Map<String, Object> variables = Map.of();

        try {
            ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult();
            if (pi != null) {
                businessKey = pi.getBusinessKey();
            }
            variables = taskService.getVariables(task.getId());
        } catch (Exception e) {
            log.warn("Could not load workflow task details for {}: {}", task.getId(), e.getMessage());
        }

        return new WorkflowTask(
                task.getId(),
                task.getName(),
                task.getTaskDefinitionKey(),
                task.getProcessInstanceId(),
                businessKey,
                task.getAssignee(),
                task.getCreateTime(),
                variables
        );
    }

    private WorkflowInstance toWorkflowInstance(ProcessInstance instance) {
        return new WorkflowInstance(
                instance.getId(),
                instance.getProcessDefinitionId(),
                instance.getBusinessKey(),
                false
        );
    }

    private WorkflowInstance toWorkflowInstance(HistoricProcessInstance instance) {
        return new WorkflowInstance(
                instance.getId(),
                instance.getProcessDefinitionId(),
                instance.getBusinessKey(),
                instance.getEndTime() != null
        );
    }
}
