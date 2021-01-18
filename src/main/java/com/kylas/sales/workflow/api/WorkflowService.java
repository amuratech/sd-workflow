package com.kylas.sales.workflow.api;

import static com.kylas.sales.workflow.domain.workflow.ConditionType.FOR_ALL;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

import com.kylas.sales.workflow.api.request.Condition;
import com.kylas.sales.workflow.api.request.FilterRequest;
import com.kylas.sales.workflow.api.request.WorkflowRequest;
import com.kylas.sales.workflow.api.response.WorkflowDetail;
import com.kylas.sales.workflow.api.response.WorkflowEntry;
import com.kylas.sales.workflow.api.response.WorkflowSummary;
import com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction;
import com.kylas.sales.workflow.common.dto.ActionDetail.ReassignAction;
import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.common.dto.User;
import com.kylas.sales.workflow.common.dto.WorkflowTrigger;
import com.kylas.sales.workflow.domain.ConditionFacade;
import com.kylas.sales.workflow.domain.WorkflowFacade;
import com.kylas.sales.workflow.domain.WorkflowFilter;
import com.kylas.sales.workflow.domain.service.ValueResolver;
import com.kylas.sales.workflow.domain.workflow.ConditionType;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.TriggerFrequency;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.WorkflowExecutedEvent;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.LeadAttribute;
import com.kylas.sales.workflow.security.AuthService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class WorkflowService {

  private final WorkflowFacade workflowFacade;
  private final AuthService authService;
  private final ConditionFacade conditionFacade;
  private final ValueResolver valueResolver;

  @Autowired
  public WorkflowService(
      WorkflowFacade workflowFacade, AuthService authService, ConditionFacade conditionFacade,
      ValueResolver valueResolver) {
    this.workflowFacade = workflowFacade;
    this.authService = authService;
    this.conditionFacade = conditionFacade;
    this.valueResolver = valueResolver;
  }

  public Mono<WorkflowSummary> create(WorkflowRequest workflowRequest) {
    workflowFacade.validate(workflowRequest);
    return workflowFacade
        .create(workflowRequest)
        .map(workflow -> new WorkflowSummary(workflow.getId()));
  }

  public List<Workflow> findActiveBy(long tenantId, EntityType entityType, TriggerFrequency triggerFrequency) {
    return workflowFacade.findActiveBy(tenantId, entityType, triggerFrequency);
  }

  public Mono<WorkflowDetail> get(long workflowId) {
    String authToken = authService.getAuthenticationToken();
    return toWorkflowDetail(workflowFacade.get(workflowId), authToken);
  }

  public Mono<WorkflowDetail> update(long workflowId, WorkflowRequest workflowRequest) {
    String authToken = authService.getAuthenticationToken();
    return workflowFacade.update(workflowId, workflowRequest)
        .flatMap(workflow -> toWorkflowDetail(workflow, authToken));
  }

  private Mono<WorkflowDetail> toWorkflowDetail(Workflow workflow, String authenticationToken) {
    var workflowTrigger =
        new WorkflowTrigger(
            workflow.getWorkflowTrigger().getTriggerType(),
            workflow.getWorkflowTrigger().getTriggerFrequency());

    ConditionType conditionType = workflow.getWorkflowCondition().getType();
    var conditionMono =
        conditionType.equals(FOR_ALL)
            ? Mono.just(new Condition(conditionType.name(), null))
            : conditionFacade.nameResolved(workflow.getWorkflowCondition().getExpression(), authenticationToken)
                .map(expression -> new Condition(conditionType.name(), conditionFacade.flattenExpression(expression)));

    var createdBy = new User(workflow.getCreatedBy().getId(), workflow.getCreatedBy().getName());
    var updatedBy = new User(workflow.getUpdatedBy().getId(), workflow.getUpdatedBy().getName());

    var executedEvent =
        nonNull(workflow.getWorkflowExecutedEvent())
            ? workflow.getWorkflowExecutedEvent()
            : WorkflowExecutedEvent.createNew(workflow);

    List<ActionResponse> actionResponses = workflow.getWorkflowActions()
        .stream()
        .map(action -> action.getType().toActionResponse(action))
        .collect(toList());

    return Flux.fromIterable(actionResponses)
        .flatMap(actionResponse -> getResolvedAction(actionResponse, authenticationToken))
        .collectList()
        .zipWith(conditionMono)
        .map(objects -> new WorkflowDetail(
            workflow.getId(), workflow.getName(), workflow.getDescription(), workflow.getEntityType(),
            workflowTrigger, objects.getT2(), objects.getT1(), createdBy, updatedBy,
            workflow.getCreatedAt(), workflow.getUpdatedAt(), executedEvent.getLastTriggeredAt(),
            executedEvent.getTriggerCount(), workflow.getAllowedActions(), workflow.isActive()));
  }

  private Mono<ActionResponse> getResolvedAction(ActionResponse action, String authenticationToken) {
    if (action.getType().equals(ActionType.REASSIGN)) {
      var actionDetail = (ReassignAction) action.getPayload();
      return valueResolver
          .getUserName(actionDetail.getId(), authenticationToken)
          .map(name ->
              new ActionResponse(
                  action.getId(),
                  action.getType(),
                  new ReassignAction(actionDetail.getId(), name)));
    }
    if (action.getType().equals(ActionType.EDIT_PROPERTY)) {
      var actionDetail = (EditPropertyAction) action.getPayload();
      if (actionDetail.getName().equals(LeadAttribute.PIPELINE.getName())) {
        valueResolver.getPipeline(actionDetail.getValue(), authenticationToken)
            .map(idName ->
                new ActionResponse(
                    action.getId(),
                    action.getType(),
                    new EditPropertyAction(actionDetail.getName(), idName, actionDetail.getValueType())));
      }
    }
    return Mono.just(action);
  }

  private WorkflowEntry toWorkflowEntry(Workflow workflow) {
    var createdBy = new User(workflow.getCreatedBy().getId(), workflow.getCreatedBy().getName());
    var updatedBy = new User(workflow.getUpdatedBy().getId(), workflow.getUpdatedBy().getName());
    var executedEvent =
        nonNull(workflow.getWorkflowExecutedEvent())
            ? workflow.getWorkflowExecutedEvent()
            : WorkflowExecutedEvent.createNew(workflow);
    return new WorkflowEntry(workflow.getId(), workflow.getName(), workflow.getEntityType(),
        createdBy, updatedBy, workflow.getCreatedAt(), workflow.getUpdatedAt(), executedEvent.getLastTriggeredAt(),
        executedEvent.getTriggerCount(), workflow.getAllowedActions(), workflow.isActive());
  }

  public Mono<Page<WorkflowDetail>> list(Pageable pageable) {
    Page<Workflow> list = workflowFacade.list(pageable);
    String authToken = authService.getAuthenticationToken();
    return Flux
        .fromIterable(list.getContent())
        .flatMap(workflow -> toWorkflowDetail(workflow, authToken))
        .collectList()
        .map(workflowDetails -> new PageImpl<>(workflowDetails, list.getPageable(), list.getTotalElements()));
  }

  public void updateExecutedEventDetails(Workflow workflow) {
    workflowFacade.updateExecutedEvent(workflow);
  }

  public Mono<WorkflowDetail> deactivate(long workflowId) {
    String authToken = authService.getAuthenticationToken();
    return toWorkflowDetail(workflowFacade.deactivate(workflowId), authToken);
  }

  public Mono<WorkflowDetail> activate(long workflowId) {
    String authToken = authService.getAuthenticationToken();
    return toWorkflowDetail(workflowFacade.activate(workflowId), authToken);
  }

  public Page<WorkflowEntry> search(
      Pageable pageable, Optional<FilterRequest> optionalFilterRequest) {
    Optional<Set<WorkflowFilter>> workflowFilters = optionalFilterRequest
        .map(filterRequest -> filterRequest.getFilters().stream()
            .map(filter -> new WorkflowFilter(filter.getOperator(), filter.getFieldName(), filter.getFieldType(), filter.getValue()))
            .collect(Collectors.toSet()));
    Page<Workflow> list = workflowFacade.search(pageable, workflowFilters);

    List<WorkflowEntry> workflowEntries = list.getContent().stream().map(this::toWorkflowEntry).collect(toList());
    return new PageImpl<>(workflowEntries, list.getPageable(), list.getTotalElements());
  }
}
