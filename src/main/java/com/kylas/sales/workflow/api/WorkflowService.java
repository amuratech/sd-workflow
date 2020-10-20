package com.kylas.sales.workflow.api;

import com.kylas.sales.workflow.api.request.WorkflowRequest;
import com.kylas.sales.workflow.api.response.WorkflowDetail;
import com.kylas.sales.workflow.api.response.WorkflowSummary;
import com.kylas.sales.workflow.common.dto.User;
import com.kylas.sales.workflow.common.dto.WorkflowAction;
import com.kylas.sales.workflow.common.dto.WorkflowCondition;
import com.kylas.sales.workflow.common.dto.WorkflowEditProperty;
import com.kylas.sales.workflow.common.dto.WorkflowTrigger;
import com.kylas.sales.workflow.domain.WorkflowFacade;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType;
import java.util.List;
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
    return workflowFacade.create(workflowRequest).map(workflow -> new WorkflowSummary(workflow.getId()));
  }

  public List<Workflow> findAllBy(long tenantId, EntityType entityType) {
    return workflowFacade.findAllBy(tenantId, entityType);
  }

  public WorkflowDetail get(long workflowId) {
    return toWorkflowDetail(workflowFacade.get(workflowId));
  }

  private WorkflowDetail toWorkflowDetail(Workflow workflow) {
    var workflowTrigger = new WorkflowTrigger(workflow.getWorkflowTrigger().getTriggerType(), workflow.getWorkflowTrigger().getTriggerFrequency());
    var workflowCondition = new WorkflowCondition(workflow.getWorkflowCondition().getType());

    var actions = workflow.getWorkflowActions()
        .stream()
        .map(abstractWorkflowAction -> (EditPropertyAction) abstractWorkflowAction)
        .map(editPropertyAction -> new WorkflowAction(
            ActionType.EDIT_PROPERTY, new WorkflowEditProperty(editPropertyAction.getName(), editPropertyAction.getValue())))
        .collect(Collectors.toSet());
    var createdBy = new User(workflow.getCreatedBy().getId(), workflow.getCreatedBy().getName());
    var updatedBy = new User(workflow.getUpdatedBy().getId(), workflow.getUpdatedBy().getName());
    return new WorkflowDetail(workflow.getId(), workflow.getName(), workflow.getDescription(), workflow.getEntityType(), workflowTrigger,
        workflowCondition,
        actions, createdBy, updatedBy, workflow.getCreatedAt(), workflow.getUpdatedAt(), workflow.getAllowedActions());
  }

  public Mono<Page<WorkflowDetail>> list(Pageable pageable) {
    Page<Workflow> list = workflowFacade.list(pageable);
    List<WorkflowDetail> workflowDetails = list.getContent()
        .stream()
        .map(workflow -> toWorkflowDetail(workflow))
        .collect(Collectors.toList());
    return Mono.just(new PageImpl<>(workflowDetails, list.getPageable(), list.getTotalElements()));
  }
}
