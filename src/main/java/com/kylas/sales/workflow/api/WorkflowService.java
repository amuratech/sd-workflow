package com.kylas.sales.workflow.api;

import static java.util.Objects.nonNull;

import com.kylas.sales.workflow.api.request.WorkflowRequest;
import com.kylas.sales.workflow.api.response.WorkflowDetail;
import com.kylas.sales.workflow.api.response.WorkflowSummary;
import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.common.dto.User;
import com.kylas.sales.workflow.common.dto.WorkflowCondition;
import com.kylas.sales.workflow.common.dto.WorkflowTrigger;
import com.kylas.sales.workflow.domain.WorkflowFacade;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.WorkflowExecutedEvent;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class WorkflowService {

  private final WorkflowFacade workflowFacade;

  @Autowired
  public WorkflowService(WorkflowFacade workflowFacade) {
    this.workflowFacade = workflowFacade;
  }

  public Mono<WorkflowSummary> create(WorkflowRequest workflowRequest) {
    return workflowFacade
        .create(workflowRequest)
        .map(workflow -> new WorkflowSummary(workflow.getId()));
  }

  public List<Workflow> findActiveBy(long tenantId, EntityType entityType) {
    return workflowFacade.findActiveBy(tenantId, entityType);
  }

  public WorkflowDetail get(long workflowId) {
    return toWorkflowDetail(workflowFacade.get(workflowId));
  }

  public Mono<WorkflowDetail> update(long workflowId, WorkflowRequest workflowRequest) {
    return workflowFacade
        .update(workflowId, workflowRequest)
        .map(this::toWorkflowDetail);
  }

  private WorkflowDetail toWorkflowDetail(Workflow workflow) {
    var workflowTrigger = new WorkflowTrigger(workflow.getWorkflowTrigger().getTriggerType(), workflow.getWorkflowTrigger().getTriggerFrequency());
    var workflowCondition = new WorkflowCondition(workflow.getWorkflowCondition().getType());

    Set<ActionResponse> actions = workflow.getWorkflowActions().stream()
        .map(workflowAction -> workflowAction.getType().toActionResponse(workflowAction))
        .collect(Collectors.toSet());

    var createdBy = new User(workflow.getCreatedBy().getId(), workflow.getCreatedBy().getName());
    var updatedBy = new User(workflow.getUpdatedBy().getId(), workflow.getUpdatedBy().getName());
    var executedEvent = nonNull(workflow.getWorkflowExecutedEvent())
        ? workflow.getWorkflowExecutedEvent()
        : WorkflowExecutedEvent.createNew(workflow);

    return new WorkflowDetail(
        workflow.getId(), workflow.getName(), workflow.getDescription(), workflow.getEntityType(), workflowTrigger,
        workflowCondition, actions, createdBy, updatedBy, workflow.getCreatedAt(), workflow.getUpdatedAt(),
        executedEvent.getLastTriggeredAt(), executedEvent.getTriggerCount(), workflow.getAllowedActions(), workflow.isActive());
  }

  public Mono<Page<WorkflowDetail>> list(Pageable pageable) {
    Page<Workflow> list = workflowFacade.list(pageable);
    List<WorkflowDetail> workflowDetails = list.getContent()
        .stream()
        .map(this::toWorkflowDetail)
        .collect(Collectors.toList());
    return Mono.just(new PageImpl<>(workflowDetails, list.getPageable(), list.getTotalElements()));
  }

  public void updateExecutedEventDetails(Workflow workflow) {
    workflowFacade.updateExecutedEvent(workflow);
  }

  public WorkflowDetail deactivate(long workflowId) {
    return toWorkflowDetail(workflowFacade.deactivate(workflowId));
  }

  public WorkflowDetail activate(long workflowId) {
    return toWorkflowDetail(workflowFacade.activate(workflowId));
  }
}
