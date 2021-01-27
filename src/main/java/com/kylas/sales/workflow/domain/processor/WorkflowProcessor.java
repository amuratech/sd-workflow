package com.kylas.sales.workflow.domain.processor;

import static java.util.Objects.isNull;

import com.kylas.sales.workflow.api.WorkflowService;
import com.kylas.sales.workflow.domain.ConditionFacade;
import com.kylas.sales.workflow.domain.processor.exception.WorkflowExecutionException;
import com.kylas.sales.workflow.domain.processor.lead.Lead;
import com.kylas.sales.workflow.domain.processor.lead.LeadDetail;
import com.kylas.sales.workflow.domain.workflow.ConditionType;
import com.kylas.sales.workflow.domain.workflow.TriggerFrequency;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.action.AbstractWorkflowAction;
import com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction;
import com.kylas.sales.workflow.domain.workflow.action.ValueConverter;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType;
import com.kylas.sales.workflow.domain.workflow.action.reassign.ReassignAction;
import com.kylas.sales.workflow.domain.workflow.action.reassign.ReassignDetail;
import com.kylas.sales.workflow.domain.workflow.action.webhook.WebhookAction;
import com.kylas.sales.workflow.domain.workflow.action.webhook.WebhookService;
import com.kylas.sales.workflow.error.ErrorCode;
import com.kylas.sales.workflow.mq.command.LeadUpdatedCommandPublisher;
import com.kylas.sales.workflow.mq.event.LeadEvent;
import com.kylas.sales.workflow.mq.event.Metadata;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WorkflowProcessor {

  private final WorkflowService workflowService;
  private final LeadUpdatedCommandPublisher leadUpdatedCommandPublisher;
  private final WebhookService webhookService;
  private final ValueConverter valueConverter;
  private final ConditionFacade conditionFacade;

  @Autowired
  public WorkflowProcessor(
      WorkflowService workflowService,
      LeadUpdatedCommandPublisher leadUpdatedCommandPublisher,
      WebhookService webhookService,
      ValueConverter valueConverter,
      ConditionFacade conditionFacade) {
    this.workflowService = workflowService;
    this.leadUpdatedCommandPublisher = leadUpdatedCommandPublisher;
    this.webhookService = webhookService;
    this.valueConverter = valueConverter;
    this.conditionFacade = conditionFacade;
  }

  public void process(LeadEvent event) {

    log.info("Lead {} event received with metadata {}", event.getMetadata().getEntityAction(), event.getMetadata());
    List<Workflow> workflows = workflowService.findActiveBy(event.getMetadata().getTenantId(), event.getMetadata().getEntityType(),
        TriggerFrequency.valueOf(event.getMetadata().getEntityAction().name()))
        .stream()
        .filter(workflow ->
            !event.getMetadata().isProcessed(workflow.getId()) && satisfiesCondition(event, workflow))
        .collect(Collectors.toList());

    var workflowIds = workflows.stream().map(Workflow::getId).collect(Collectors.toSet());

    workflows.stream().forEach(workflow -> {
      LeadDetail entity = event.getEntity();
      Metadata metadata = event.getMetadata().with(workflow.getId()).withAllWorkflowIds(workflowIds).withEntityId(entity.getId());
      Set<AbstractWorkflowAction> workflowActions = workflow.getWorkflowActions();
      log.info("Workflow execution start for workflowId {} and prev metadata {}", workflow.getId(), event.getMetadata());
      processActions(metadata, workflowActions, entity);
      workflowService.updateExecutedEventDetails(workflow);
    });
  }

  private boolean satisfiesCondition(LeadEvent event, Workflow workflow) {
    return isNull(workflow.getWorkflowCondition()) ||
        workflow.getWorkflowCondition().getType().equals(ConditionType.FOR_ALL) ||
        conditionFacade.satisfies(workflow.getWorkflowCondition().getExpression(), event.getEntity());
  }

  private void processActions(Metadata metadata, final Set<AbstractWorkflowAction> workflowActions, LeadDetail entity) {
    Set<EditPropertyAction> editPropertyActions = workflowActions.stream()
        .filter(workflowAction -> workflowAction.getType().equals(ActionType.EDIT_PROPERTY))
        .map(workflowAction -> (EditPropertyAction) workflowAction).collect(
            Collectors.toSet());

    if (!editPropertyActions.isEmpty()) {
      processEditPropertyActions(editPropertyActions, metadata);
    }

    workflowActions.stream().filter(workflowAction -> workflowAction.getType().equals(ActionType.WEBHOOK))
        .map(workflowAction -> (WebhookAction) workflowAction).forEach(webhookAction ->
        webhookService.execute(webhookAction, entity));

    workflowActions.stream().filter(workflowAction -> workflowAction.getType().equals(ActionType.REASSIGN))
        .map(workflowAction -> (ReassignAction) workflowAction).findFirst().ifPresent(
        reassignAction -> leadUpdatedCommandPublisher.execute(metadata, convertToReassignDetail(entity.getId(), reassignAction.getOwnerId())));
  }

  private void processEditPropertyActions(Set<EditPropertyAction> editPropertyActions, Metadata metadata) {
    Lead lead = new Lead();
    editPropertyActions.forEach(editPropertyAction -> {
      try {
        log.info("Executing EditPropertyAction with Id {}, name {} and value {} ", editPropertyAction.getId(), editPropertyAction.getName(),
            editPropertyAction.getValue());
        EvaluationContext context = SimpleEvaluationContext.forReadWriteDataBinding().build();
        ExpressionParser parser = new SpelExpressionParser();
        parser.parseExpression(editPropertyAction.getName())
            .setValue(context, lead, valueConverter.getValue(editPropertyAction, Lead.class.getDeclaredField(editPropertyAction.getName())));
      } catch (SpelEvaluationException e) {
        log.error("Exception for EditPropertyAction with Id {}, name {} and value {} with errorMessage {} ", editPropertyAction.getId(),
            editPropertyAction.getName(),
            editPropertyAction.getValue(), e.getMessageCode());
        throw new WorkflowExecutionException(ErrorCode.UPDATE_PROPERTY);
      } catch (NoSuchFieldException e) {
        log.error("Exception for EditPropertyAc"
                + "tion with Id {}, name {} and value {} with errorMessage {} ", editPropertyAction.getId(),
            editPropertyAction.getName(),
            editPropertyAction.getValue(), e.getMessage());
      }
    });
    log.info("Publishing command to execute edit property actions on entity with Id {}, with new metadata {} ", metadata.getEntityId(), metadata);
    leadUpdatedCommandPublisher.execute(metadata, lead);
  }

  private ReassignDetail convertToReassignDetail(Long entityId, Long ownerId) {
    return new ReassignDetail(entityId, ownerId);
  }

}
