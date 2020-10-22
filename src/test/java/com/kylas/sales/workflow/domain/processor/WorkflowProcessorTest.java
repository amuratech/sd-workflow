package com.kylas.sales.workflow.domain.processor;

import static com.kylas.sales.workflow.domain.workflow.EntityType.LEAD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.kylas.sales.workflow.api.WorkflowService;
import com.kylas.sales.workflow.domain.processor.lead.Lead;
import com.kylas.sales.workflow.domain.workflow.TriggerFrequency;
import com.kylas.sales.workflow.domain.workflow.TriggerType;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.WorkflowTrigger;
import com.kylas.sales.workflow.domain.workflow.action.AbstractWorkflowAction;
import com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction;
import com.kylas.sales.workflow.mq.command.LeadUpdatedCommandPublisher;
import com.kylas.sales.workflow.mq.event.EntityAction;
import com.kylas.sales.workflow.mq.event.LeadCreatedEvent;
import com.kylas.sales.workflow.mq.event.Metadata;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class WorkflowProcessorTest {

  @Mock
  private WorkflowService workflowService;
  @Mock
  private LeadUpdatedCommandPublisher leadUpdatedCommandPublisher;
  @InjectMocks
  private WorkflowProcessor workflowProcessor;

  @Test
  public void givenLeadEvent_shouldPublishPatchCommand() {
    //given
    long tenantId = 101;
    long workflowId = 99L;

    var lead = new Lead();
    lead.setId(55L);
    lead.setFirstName("Tony");
    lead.setLastName("Stark");
    var metadata = mock(Metadata.class);
    var updatedMetadata = mock(Metadata.class);
    given(metadata.getTenantId()).willReturn(tenantId);
    given(metadata.getEntityType()).willReturn(LEAD);

    var updatedMetadataWithEntityId = mock(Metadata.class);
    given(updatedMetadataWithEntityId.getTenantId()).willReturn(tenantId);
    given(updatedMetadataWithEntityId.getEntityType()).willReturn(LEAD);

    given(metadata.with(workflowId)).willReturn(updatedMetadata);
    given(updatedMetadata.withEntityId(55L)).willReturn(updatedMetadataWithEntityId);
    given(metadata.getEntityAction()).willReturn(EntityAction.CREATED);

    var leadCreatedEvent = new LeadCreatedEvent(lead, null, metadata);

    Workflow workflowMock = mock(Workflow.class);
    given(workflowMock.getId()).willReturn(workflowId);

    EditPropertyAction workflowAction = mock(EditPropertyAction.class);
    given(workflowAction.getName()).willReturn("firstName");
    given(workflowAction.getValue()).willReturn("Steve");
    Actionable actionableMock = mock(Actionable.class);
    given(workflowAction.process(any(Lead.class))).willReturn(actionableMock);
    Set<AbstractWorkflowAction> actions = new HashSet<>();
    actions.add(workflowAction);

    given(workflowMock.getWorkflowActions()).willReturn(actions);
    WorkflowTrigger workflowTriggerMock = mock(WorkflowTrigger.class);
    given(workflowTriggerMock.getTriggerType()).willReturn(TriggerType.EVENT);
    given(workflowTriggerMock.getTriggerFrequency()).willReturn(TriggerFrequency.CREATED);
    given(workflowMock.getWorkflowTrigger()).willReturn(workflowTriggerMock);

    List<Workflow> workflows = Arrays.asList(workflowMock);

    given(workflowService.findAllBy(tenantId, LEAD)).willReturn(workflows);
    doNothing().when(leadUpdatedCommandPublisher).execute(any(Metadata.class), any(Lead.class));
    //when
    workflowProcessor.process(leadCreatedEvent);
    //then
    verify(workflowAction, times(1)).process(any(Lead.class));
    verify(leadUpdatedCommandPublisher, times(1)).execute(any(Metadata.class), any(Actionable.class));
    verify(workflowService, times(1)).updateExecutedEventDetails(any(Workflow.class));
  }

  @Test
  public void givenMultipleWorkflow_whereOneIsFailing_otherShouldBeExecute() {
//given
    long tenantId = 101;
    long workflowId = 99L;

    var lead = new Lead();
    lead.setFirstName("Tony");
    lead.setLastName("Stark");
    lead.setId(55L);
    var metadata = mock(Metadata.class);
    var updatedMetadata = mock(Metadata.class);
    given(metadata.getTenantId()).willReturn(tenantId);
    given(metadata.getEntityType()).willReturn(LEAD);

    var updatedMetadataWithEntityId = mock(Metadata.class);
    given(updatedMetadataWithEntityId.getTenantId()).willReturn(tenantId);
    given(updatedMetadataWithEntityId.getEntityType()).willReturn(LEAD);

    given(metadata.with(workflowId)).willReturn(updatedMetadata);
    given(updatedMetadata.withEntityId(55L)).willReturn(updatedMetadataWithEntityId);

    given(metadata.getEntityAction()).willReturn(EntityAction.CREATED);

    var leadCreatedEvent = new LeadCreatedEvent(lead, null, metadata);

    Workflow workflowMock = mock(Workflow.class);
    given(workflowMock.getId()).willReturn(workflowId);

    Set<AbstractWorkflowAction> actions = new HashSet<>();

    EditPropertyAction updateFirstNameAction = mock(EditPropertyAction.class);
    given(updateFirstNameAction.getName()).willReturn("firstName");
    given(updateFirstNameAction.getValue()).willReturn("Steve");
    Actionable actionableMock = mock(Actionable.class);
    given(updateFirstNameAction.process(any(Lead.class))).willReturn(actionableMock);
    actions.add(updateFirstNameAction);

    EditPropertyAction failedAction = mock(EditPropertyAction.class);
    given(failedAction.getName()).willReturn("firstName");
    given(failedAction.getValue()).willReturn("Steve");
    given(failedAction.process(lead)).willThrow(SpelEvaluationException.class);
    actions.add(failedAction);

    given(workflowMock.getWorkflowActions()).willReturn(actions);
    WorkflowTrigger workflowTriggerMock = mock(WorkflowTrigger.class);
    given(workflowTriggerMock.getTriggerType()).willReturn(TriggerType.EVENT);
    given(workflowTriggerMock.getTriggerFrequency()).willReturn(TriggerFrequency.CREATED);
    given(workflowMock.getWorkflowTrigger()).willReturn(workflowTriggerMock);

    List<Workflow> workflows = Arrays.asList(workflowMock);

    given(workflowService.findAllBy(tenantId, LEAD)).willReturn(workflows);
    doNothing().when(leadUpdatedCommandPublisher).execute(any(Metadata.class), any(Lead.class));
    //when
    workflowProcessor.process(leadCreatedEvent);
    //then
    verify(updateFirstNameAction, times(1)).process(any(Lead.class));
    verify(failedAction, times(1)).process(any(Lead.class));
    verify(leadUpdatedCommandPublisher, times(1)).execute(any(Metadata.class), any(Actionable.class));
  }


}