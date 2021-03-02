package com.kylas.sales.workflow.integration;

import static com.kylas.sales.workflow.domain.workflow.TriggerFrequency.CREATED;
import static com.kylas.sales.workflow.domain.workflow.TriggerFrequency.UPDATED;
import static com.kylas.sales.workflow.domain.workflow.TriggerType.EVENT;

import com.kylas.sales.workflow.common.dto.WorkflowTrigger;
import com.kylas.sales.workflow.domain.exception.InvalidWorkflowRequestException;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.integration.request.IntegrationRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class IntegrationConfig {

  private final EntityType entityType;
  private final WorkflowTrigger trigger;
  private final String hookUrl;

  private IntegrationConfig(EntityType entityType, WorkflowTrigger trigger, String hookUrl) {
    this.entityType = entityType;
    this.trigger = trigger;
    this.hookUrl = hookUrl;
  }

  public static IntegrationConfig from(String event, IntegrationRequest request) {
    var eventParts = event.split("-");
    if (eventParts.length != 2) {
      log.error("Invalid workflow integration url.");
      throw new InvalidWorkflowRequestException();
    }
    var entityType = EntityType.valueOf(eventParts[1].toUpperCase());
    if (!entityType.isIntegrationAllowed()) {
      log.error("Integration is not allowed on entity type {}", entityType);
      throw new InvalidWorkflowRequestException();
    }
    var triggerFrequency = eventParts[0].equalsIgnoreCase("create") ? CREATED : UPDATED;
    WorkflowTrigger trigger = new WorkflowTrigger(EVENT, triggerFrequency);
    return new IntegrationConfig(entityType, trigger, request.getHookUrl());
  }
}
