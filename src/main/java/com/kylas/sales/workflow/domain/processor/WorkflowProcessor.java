package com.kylas.sales.workflow.domain.processor;

import static com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.FieldValueType.companyPhones;
import static com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.FieldValueType.emails;
import static com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.FieldValueType.phoneNumbers;
import static com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.FieldValueType.products;
import static com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.FieldValueType.valueOf;
import static com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.ValueType.ARRAY;
import static java.util.Collections.emptyList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylas.sales.workflow.api.WorkflowService;
import com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.FieldValueType;
import com.kylas.sales.workflow.domain.processor.exception.WorkflowExecutionException;
import com.kylas.sales.workflow.domain.processor.lead.Email;
import com.kylas.sales.workflow.domain.processor.lead.Lead;
import com.kylas.sales.workflow.domain.processor.lead.LeadDetail;
import com.kylas.sales.workflow.domain.processor.lead.PhoneNumber;
import com.kylas.sales.workflow.domain.processor.lead.Product;
import com.kylas.sales.workflow.domain.workflow.TriggerFrequency;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.action.AbstractWorkflowAction;
import com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType;
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

    log.info("Lead {} event received with metadata {}", event.getMetadata().getEntityAction(), event.getMetadata());
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
      processActions(metadata, workflowActions, entity);
      workflowService.updateExecutedEventDetails(workflow);
    });
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
            .setValue(context, lead,
                getValue(editPropertyAction));
      } catch (SpelEvaluationException e) {
        log.error("Exception for EditPropertyAction with Id {}, name {} and value {} with errorMessage {} ", editPropertyAction.getId(),
            editPropertyAction.getName(),
            editPropertyAction.getValue(), e.getMessageCode());
        throw new WorkflowExecutionException(ErrorCode.UPDATE_PROPERTY);
      }
      log.info("Publishing command to execute actionId {}, with new metadata {} ", editPropertyAction.getId(), metadata);
    });
    leadUpdatedCommandPublisher.execute(metadata, lead);
  }

  private Object getValue(EditPropertyAction editPropertyAction) {
    return ARRAY.equals(editPropertyAction.getValueType()) ? convertToList(valueOf(editPropertyAction.getName()),
        editPropertyAction.getValue())
        : editPropertyAction.getValue();
  }

  private List<?> convertToList(FieldValueType fieldValueType, Object value) {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      if (fieldValueType.equals(products)) {
        return objectMapper.readValue(String.valueOf(value), new TypeReference<List<Product>>() {
        });
      }
      if (fieldValueType.equals(phoneNumbers) || fieldValueType.equals(companyPhones)) {
        return objectMapper.readValue(String.valueOf(value), new TypeReference<List<PhoneNumber>>() {
        });
      }
      if (fieldValueType.equals(emails)) {
        return objectMapper.readValue(String.valueOf(value), new TypeReference<List<Email>>() {
        });
      }
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return emptyList();
  }
}
