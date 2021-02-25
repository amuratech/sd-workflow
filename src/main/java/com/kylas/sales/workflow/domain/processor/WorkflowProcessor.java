package com.kylas.sales.workflow.domain.processor;

import static java.util.Objects.isNull;

import com.kylas.sales.workflow.api.WorkflowService;
import com.kylas.sales.workflow.domain.ConditionFacade;
import com.kylas.sales.workflow.domain.processor.exception.WorkflowExecutionException;
import com.kylas.sales.workflow.domain.workflow.ConditionType;
import com.kylas.sales.workflow.domain.workflow.TriggerFrequency;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.action.AbstractWorkflowAction;
import com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction;
import com.kylas.sales.workflow.domain.workflow.action.ValueConverter;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType;
import com.kylas.sales.workflow.domain.workflow.action.reassign.ReassignAction;
import com.kylas.sales.workflow.domain.workflow.action.reassign.ReassignDetail;
import com.kylas.sales.workflow.domain.workflow.action.task.CreateTaskAction;
import com.kylas.sales.workflow.domain.workflow.action.task.CreateTaskService;
import com.kylas.sales.workflow.domain.workflow.action.webhook.WebhookAction;
import com.kylas.sales.workflow.domain.workflow.action.webhook.WebhookService;
import com.kylas.sales.workflow.error.ErrorCode;
import com.kylas.sales.workflow.mq.command.EntityUpdatedCommandPublisher;
import com.kylas.sales.workflow.mq.event.EntityEvent;
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
  private final EntityUpdatedCommandPublisher entityUpdatedCommandPublisher;
  private final WebhookService webhookService;
  private final ValueConverter valueConverter;
  private final ConditionFacade conditionFacade;
  private final CreateTaskService createTaskService;

  @Autowired
  public WorkflowProcessor(
      WorkflowService workflowService,
      EntityUpdatedCommandPublisher entityUpdatedCommandPublisher,
      WebhookService webhookService,
      ValueConverter valueConverter,
      ConditionFacade conditionFacade,
      CreateTaskService createTaskService) {
    this.workflowService = workflowService;
    this.entityUpdatedCommandPublisher = entityUpdatedCommandPublisher;
    this.webhookService = webhookService;
    this.valueConverter = valueConverter;
    this.conditionFacade = conditionFacade;
    this.createTaskService = createTaskService;
  }

  public void process(EntityEvent event) {

    Metadata metadata = event.getMetadata();
    log.info("{} {} event received with metadata {}", metadata.getEntityType(), metadata.getEntityAction(), metadata);
    List<Workflow> workflows = workflowService.findActiveBy(metadata.getTenantId(), metadata.getEntityType(),
        TriggerFrequency.valueOf(metadata.getEntityAction().name()))
        .stream()
        .filter(workflow ->
            !metadata.isProcessed(workflow.getId()) && satisfiesCondition(event, workflow))
        .collect(Collectors.toList());

    var workflowIds = workflows.stream().map(Workflow::getId).collect(Collectors.toSet());

    workflows.stream()
        .forEach(workflow -> {
          Metadata updatedMetadata = metadata.with(workflow.getId()).withAllWorkflowIds(workflowIds)
              .withEntityId(event.getEntityId());
          Set<AbstractWorkflowAction> workflowActions = workflow.getWorkflowActions();
          log.info("Workflow execution start for workflowId {} and prev metadata {}", workflow.getId(), metadata);
          processActions(updatedMetadata, workflowActions, event);
          workflowService.updateExecutedEventDetails(workflow);
        });
  }

  private boolean satisfiesCondition(EntityEvent event, Workflow workflow) {
    return isNull(workflow.getWorkflowCondition()) ||
        workflow.getWorkflowCondition().getType().equals(ConditionType.FOR_ALL) ||
        conditionFacade.satisfies(workflow.getWorkflowCondition().getExpression(), event.getEntity());
  }

  private void processActions(Metadata metadata, final Set<AbstractWorkflowAction> workflowActions, EntityEvent event) {
    Set<EditPropertyAction> editPropertyActions = workflowActions.stream()
        .filter(workflowAction -> workflowAction.getType().equals(ActionType.EDIT_PROPERTY))
        .map(workflowAction -> (EditPropertyAction) workflowAction).collect(
            Collectors.toSet());

    if (!editPropertyActions.isEmpty()) {
      processEditPropertyActions(editPropertyActions, metadata, event.getActualEntity());
    }

    workflowActions.stream().filter(workflowAction -> workflowAction.getType().equals(ActionType.WEBHOOK))
        .map(workflowAction -> (WebhookAction) workflowAction).forEach(webhookAction ->
        webhookService.execute(webhookAction, event.getEntity(), event.getMetadata().getEntityType()));

    workflowActions.stream().filter(workflowAction -> workflowAction.getType().equals(ActionType.REASSIGN))
        .map(workflowAction -> (ReassignAction) workflowAction).findFirst().ifPresent(
        reassignAction -> entityUpdatedCommandPublisher
            .execute(metadata, new ReassignDetail(event.getEntityId(), reassignAction.getOwnerId(), metadata.getEntityType())));

    workflowActions.stream().filter(workflowAction -> workflowAction.getType().equals(ActionType.CREATE_TASK))
        .map(workflowAction -> (CreateTaskAction) workflowAction)
        .forEach(createTaskAction -> {
          com.kylas.sales.workflow.domain.processor.task.Metadata createTaskMetadata = new com.kylas.sales.workflow.domain.processor.task.Metadata(
              metadata.getUserId(), metadata.getTenantId());
          createTaskService.processCreateTaskAction(createTaskAction, metadata.getEntityType(), event.getEntity(), createTaskMetadata);
        });
  }

  private void processEditPropertyActions(Set<EditPropertyAction> editPropertyActions, Metadata metadata, Actionable entity) {
    editPropertyActions.forEach(editPropertyAction -> {
      try {
        log.info("Executing EditPropertyAction with Id {}, name {} and value {} ", editPropertyAction.getId(), editPropertyAction.getName(),
            editPropertyAction.getValue());
        EvaluationContext context = SimpleEvaluationContext.forReadWriteDataBinding().build();
        ExpressionParser parser = new SpelExpressionParser();
        parser.parseExpression(editPropertyAction.getName())
            .setValue(context, entity,
                valueConverter.getValue(editPropertyAction, entity.getClass().getDeclaredField(editPropertyAction.getName()),
                    metadata.getEntityType()));
      } catch (SpelEvaluationException e) {
        log.error("Exception for EditPropertyAction with Id {}, name {} and value {} with errorMessage {} ", editPropertyAction.getId(),
            editPropertyAction.getName(),
            editPropertyAction.getValue(), e.getMessageCode());
        throw new WorkflowExecutionException(ErrorCode.UPDATE_PROPERTY);
      } catch (NoSuchFieldException e) {
        log.error("Exception for EditPropertyAction with Id {}, name {} and value {} with errorMessage {} ", editPropertyAction.getId(),
            editPropertyAction.getName(),
            editPropertyAction.getValue(), e.getMessage());
      }
    });
    log.info("Publishing command to execute edit property actions on entity {} with Id {}, with new metadata {} ", metadata.getEntityType(),
        metadata.getEntityId(), metadata);
    entityUpdatedCommandPublisher.execute(metadata, entity);
  }
}
