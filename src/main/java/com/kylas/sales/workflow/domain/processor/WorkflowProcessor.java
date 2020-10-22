package com.kylas.sales.workflow.domain.processor;

import static com.kylas.sales.workflow.domain.workflow.TriggerFrequency.CREATED;

import com.kylas.sales.workflow.api.WorkflowService;
import com.kylas.sales.workflow.domain.processor.exception.WorkflowExecutionException;
import com.kylas.sales.workflow.domain.processor.lead.Lead;
import com.kylas.sales.workflow.domain.workflow.action.AbstractWorkflowAction;
import com.kylas.sales.workflow.mq.command.LeadUpdatedCommandPublisher;
import com.kylas.sales.workflow.mq.event.EntityAction;
import com.kylas.sales.workflow.mq.event.LeadCreatedEvent;
import com.kylas.sales.workflow.mq.event.Metadata;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WorkflowProcessor {

  private final WorkflowService workflowService;
  private final LeadUpdatedCommandPublisher leadUpdatedCommandPublisher;

  @Autowired
  public WorkflowProcessor(WorkflowService workflowService,
      LeadUpdatedCommandPublisher leadUpdatedCommandPublisher) {
    this.workflowService = workflowService;
    this.leadUpdatedCommandPublisher = leadUpdatedCommandPublisher;
  }

  public void process(LeadCreatedEvent event) {

    log.info("Lead created event received with metadata {}", event.getMetadata());
    workflowService.findAllBy(event.getMetadata().getTenantId(), event.getMetadata().getEntityType())
        .stream()
        .filter(workflow -> event.getMetadata().getEntityAction().equals(EntityAction.CREATED))
        .filter(workflow -> workflow.getWorkflowTrigger().getTriggerFrequency().equals(CREATED))
        .forEach(workflow -> {
          Lead entity = event.getEntity();
          Metadata metadata = event.getMetadata().with(workflow.getId()).withEntityId(entity.getId());
          Set<AbstractWorkflowAction> workflowActions = workflow.getWorkflowActions();
          log.info("Workflow execution start for workflowId {} and prev metadata {}", workflow.getId(), event.getMetadata());
          workflowActions.stream()
              .forEach(workflowAction -> {
                try {
                  Lead lead = new Lead();
                  Actionable actionable = workflowAction.process(lead);
                  log.info("Publishing command to execute actionId {}, with new metadata {} ", workflowAction.getId(), metadata);
                  leadUpdatedCommandPublisher.execute(metadata, actionable);
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
}
