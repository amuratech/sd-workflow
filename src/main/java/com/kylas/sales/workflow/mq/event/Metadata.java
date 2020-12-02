package com.kylas.sales.workflow.mq.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class Metadata {

  private long tenantId;
  private long userId;
  private EntityType entityType;
  private EntityAction entityAction;
  private String workflowId;
  private Set<String> executedWorkflows;
  private long entityId;
  @JsonCreator
  public Metadata(@JsonProperty("tenantId") long tenantId,
      @JsonProperty("userId") long userId,
      @JsonProperty("entityType") EntityType entityType,
      @JsonProperty("workflowId") String workflowId,
      @JsonProperty("executedWorkflows") Set<String> executedWorkflows,
      @JsonProperty("entityAction") EntityAction entityAction) {
    this.tenantId = tenantId;
    this.userId = userId;
    this.entityType = entityType;
    this.workflowId = workflowId;
    this.executedWorkflows = executedWorkflows == null ? new HashSet<>() : executedWorkflows;
    this.entityAction = entityAction;
  }

  public Metadata with(Long id) {
    Set<String> workflows = new HashSet<>(executedWorkflows);
    if (workflowId != null) {
      workflows.add(workflowId);
    }
    return new Metadata(this.tenantId, this.userId, this.entityType, getWorkflowId(id), workflows, this.entityAction);
  }
  public Metadata withEntityId(long entityId){
    Metadata metadata = new Metadata(this.tenantId, this.userId, this.entityType, workflowId, executedWorkflows, this.entityAction);
    metadata.entityId = entityId;
    return metadata;
  }

  private String getWorkflowId(long workflowId) {
    return String.format("WF_%d", workflowId);
  }

  public boolean isProcessed(Long workflowId) {
    return executedWorkflows.contains(String.format("WF_%d", workflowId));
  }
}
