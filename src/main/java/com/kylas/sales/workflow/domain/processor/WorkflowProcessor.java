package com.kylas.sales.workflow.domain.processor;

import com.kylas.sales.workflow.api.WorkflowService;
import com.kylas.sales.workflow.domain.processor.exception.WorkflowExecutionException;
import com.kylas.sales.workflow.domain.processor.lead.Lead;
import com.kylas.sales.workflow.domain.processor.lead.LeadDetail;
import com.kylas.sales.workflow.domain.workflow.TriggerFrequency;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.action.AbstractWorkflowAction;
import com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction;
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

  @Autowired
  public WorkflowProcessor(WorkflowService workflowService,
      LeadUpdatedCommandPublisher leadUpdatedCommandPublisher, WebhookService webhookService) {
    this.workflowService = workflowService;
    this.leadUpdatedCommandPublisher = leadUpdatedCommandPublisher;
    this.webhookService = webhookService;
  }

  public void process(LeadEvent event) {

    log.info("Lead {} event received with metadata {}", event.getMetadata().getEntityAction(),event.getMetadata());
    List<Workflow> workflows = workflowService.findActiveBy(event.getMetadata().getTenantId(), event.getMetadata().getEntityType(),
        TriggerFrequency.valueOf(event.getMetadata().getEntityAction().name()))
        .stream()
        .filter(workflow -> !event.getMetadata().isProcessed(workflow.getId())).collect(Collectors.toList());

      Set<Long> workflowIds = workflows.stream().map(Workflow::getId).collect(Collectors.toSet());

        workflows.forEach(workflow -> {
          LeadDetail entity = event.getEntity();
          Metadata metadata = event.getMetadata().with(workflow.getId()).withAllWorkflowIds(workflowIds).withEntityId(entity.getId());
          Set<AbstractWorkflowAction> workflowActions = workflow.getWorkflowActions();
          log.info("Workflow execution start for workflowId {} and prev metadata {}", workflow.getId(), event.getMetadata());
          workflowActions.stream()
              .forEach(workflowAction -> {
                try {
                  processAction(metadata, workflowAction, entity);
                  workflowService.updateExecutedEventDetails(workflow);
                } catch (WorkflowExecutionException e) {
                  log.error(e.getMessage());
                } catch (Exception e) {
                  log.error("Exception while executing workflow Id {} with new metadata {} and errorMessage {} ", workflow.getId(), metadata,
                      e.getMessage());
                  log.error(e.getMessage(), e);
                }
              });
        });
  }

  private void processAction(Metadata metadata, AbstractWorkflowAction workflowAction, LeadDetail entity) {
    switch (workflowAction.getType()) {
      case EDIT_PROPERTY:
        processEditProperty((EditPropertyAction) workflowAction, metadata);
        break;
      case WEBHOOK:
        webhookService.execute((WebhookAction) workflowAction, entity);
        break;
    }
  }

  private void processEditProperty(EditPropertyAction action, Metadata metadata) {
    Lead lead = new Lead();
    try {
      log.info("Executing EditPropertyAction with Id {}, name {} and value {} ", action.getId(), action.getName(), action.getValue());
      EvaluationContext context = SimpleEvaluationContext.forReadWriteDataBinding().build();
      ExpressionParser parser = new SpelExpressionParser();
      parser.parseExpression(action.getName()).setValue(context, lead, action.getValue());
    } catch (SpelEvaluationException e) {
      log.error("Exception for EditPropertyAction with Id {}, name {} and value {} with errorMessage {} ", action.getId(), action.getName(),
          action.getValue(), e.getMessageCode());
      log.error(e.getMessage(), e);
      throw new WorkflowExecutionException(ErrorCode.UPDATE_PROPERTY);
    }
    log.info("Publishing command to execute actionId {}, with new metadata {} ", action.getId(), metadata);
    leadUpdatedCommandPublisher.execute(metadata, lead);
  }
}
