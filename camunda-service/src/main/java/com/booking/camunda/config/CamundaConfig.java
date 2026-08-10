package com.booking.camunda.config;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class CamundaConfig {

    @Bean
    public CommandLineRunner logDeployments(RepositoryService repositoryService,
                                            RuntimeService runtimeService,
                                            TaskService taskService,
                                            HistoryService historyService) {
        return args -> {
            log.info("Camunda engine status");
            log.info("Deployments: {}", repositoryService.createDeploymentQuery().count());
            log.info("Process Definitions: {}", repositoryService.createProcessDefinitionQuery().count());
            log.info("Active Process Instances: {}", runtimeService.createProcessInstanceQuery().count());
            log.info("Active Tasks: {}", taskService.createTaskQuery().count());
        };
    }
}
