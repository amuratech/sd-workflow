package com.kylas.sales.workflow.domain.processor;

import static com.kylas.sales.workflow.domain.workflow.EntityType.LEAD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.kylas.sales.workflow.api.WorkflowService;
import com.kylas.sales.workflow.domain.processor.lead.Lead;
import com.kylas.sales.workflow.domain.processor.lead.LeadDetail;
import com.kylas.sales.workflow.domain.workflow.TriggerFrequency;
import com.kylas.sales.workflow.domain.workflow.TriggerType;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.WorkflowTrigger;
import com.kylas.sales.workflow.domain.workflow.action.AbstractWorkflowAction;
import com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType;
import com.kylas.sales.workflow.mq.command.LeadUpdatedCommandPublisher;
import com.kylas.sales.workflow.mq.event.EntityAction;
import com.kylas.sales.workflow.mq.event.LeadEvent;
import com.kylas.sales.workflow.mq.event.Metadata;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class WorkflowProcessorTest {

  @Mock private WorkflowService workflowService;
  @Mock private LeadUpdatedCommandPublisher leadUpdatedCommandPublisher;
  @InjectMocks private WorkflowProcessor workflowProcessor;

  @Test
  public void givenLeadEvent_shouldPublishPatchCommand() {
    // given
    long tenantId = 101;
    long workflowId = 99L;

    var lead = new LeadDetail();
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

    var leadCreatedEvent = new LeadEvent(lead, null, metadata);

    Workflow workflowMock =
        getMockEditPropertyWorkflow(workflowId, TriggerFrequency.CREATED, "firstName", "steve");

    List<Workflow> workflows = Arrays.asList(workflowMock);

    given(workflowService.findActiveBy(tenantId, LEAD, TriggerFrequency.CREATED)).willReturn(workflows);
    doNothing().when(leadUpdatedCommandPublisher).execute(any(Metadata.class), any(Lead.class));
    // when
    workflowProcessor.process(leadCreatedEvent);
    // then
    verify(leadUpdatedCommandPublisher, times(1))
        .execute(any(Metadata.class), any(Actionable.class));
    verify(workflowService, times(1)).updateExecutedEventDetails(any(Workflow.class));
  }

  @Test
  public void givenMultipleWorkflow_whereOneIsFailing_otherShouldBeExecute() {
    // given
    long tenantId = 101;
    long workflowId = 99L;

    var lead = new LeadDetail();
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

    var leadCreatedEvent = new LeadEvent(lead, null, metadata);

    Workflow workflowMock = mock(Workflow.class);
    given(workflowMock.getId()).willReturn(workflowId);

    Set<AbstractWorkflowAction> actions = new HashSet<>();

    EditPropertyAction updateFirstNameAction = mock(EditPropertyAction.class);
    given(updateFirstNameAction.getName()).willReturn("firstName");
    given(updateFirstNameAction.getValue()).willReturn("Steve");
    given(updateFirstNameAction.getType()).willReturn(ActionType.EDIT_PROPERTY);
    Actionable actionableMock = mock(Actionable.class);
    actions.add(updateFirstNameAction);

    EditPropertyAction failedAction = mock(EditPropertyAction.class);
    given(failedAction.getName()).willReturn("firstName");
    given(failedAction.getValue()).willReturn("Steve");
    given(updateFirstNameAction.getType()).willReturn(ActionType.EDIT_PROPERTY);
    actions.add(failedAction);

    given(workflowMock.getWorkflowActions()).willReturn(actions);
    WorkflowTrigger workflowTriggerMock = mock(WorkflowTrigger.class);
    given(workflowTriggerMock.getTriggerType()).willReturn(TriggerType.EVENT);
    given(workflowTriggerMock.getTriggerFrequency()).willReturn(TriggerFrequency.CREATED);
    given(workflowMock.getWorkflowTrigger()).willReturn(workflowTriggerMock);

    List<Workflow> workflows = Arrays.asList(workflowMock);

    given(workflowService.findActiveBy(tenantId, LEAD, TriggerFrequency.CREATED)).willReturn(workflows);
    doNothing().when(leadUpdatedCommandPublisher).execute(any(Metadata.class), any(Lead.class));
    // when
    workflowProcessor.process(leadCreatedEvent);
    // then
    verify(leadUpdatedCommandPublisher, times(1))
        .execute(any(Metadata.class), any(Actionable.class));
  }

  @Test
  public void givenLeadUpdatedEvent_tryToProcess_shouldExecuteWorkflowWithUpdatedFrequency() {
    // given
    long tenantId = 101;
    long workflowId99 = 99L;
    long workflowId100 = 99L;

    var lead = new LeadDetail();
    lead.setId(55L);
    lead.setFirstName("Johny");
    lead.setLastName("Stark");

    var old = new LeadDetail();
    old.setId(55L);
    old.setFirstName("Steve");
    old.setLastName("Roger");

    var metadata = mock(Metadata.class);
    var updatedMetadata = mock(Metadata.class);
    given(metadata.getTenantId()).willReturn(tenantId);
    given(metadata.getEntityType()).willReturn(LEAD);

    var updatedMetadataWithEntityId = mock(Metadata.class);
    given(updatedMetadataWithEntityId.getTenantId()).willReturn(tenantId);
    given(updatedMetadataWithEntityId.getEntityType()).willReturn(LEAD);

    given(metadata.with(workflowId99)).willReturn(updatedMetadata);
    given(updatedMetadata.withEntityId(55L)).willReturn(updatedMetadataWithEntityId);
    given(metadata.getEntityAction()).willReturn(EntityAction.UPDATED);

    var leadUpdatedEvent = new LeadEvent(lead, old, metadata);

    Workflow workflowMockUpdate =
        getMockEditPropertyWorkflow(workflowId100, TriggerFrequency.UPDATED, "firstName", "tony");

    List<Workflow> workflows = Arrays.asList(workflowMockUpdate);

    given(workflowService.findActiveBy(tenantId, LEAD, TriggerFrequency.UPDATED)).willReturn(workflows);
    doNothing().when(leadUpdatedCommandPublisher).execute(any(Metadata.class), any(Lead.class));
    // when
    workflowProcessor.process(leadUpdatedEvent);
    // then
    verify(leadUpdatedCommandPublisher, times(1))
        .execute(any(Metadata.class), any(Actionable.class));
    ArgumentCaptor<Workflow> workflowArgumentCaptor = ArgumentCaptor.forClass(Workflow.class);
    verify(workflowService, times(1)).updateExecutedEventDetails(workflowArgumentCaptor.capture());
    Workflow executedWorkflow = workflowArgumentCaptor.getValue();
    Assertions.assertThat(executedWorkflow.getId()).isEqualTo(workflowId100);
  }


  @Test
  public void givenLeadUpdatedEvent_tryExecuteSameWorkflow_shouldNotProcess() {
    // given
    long tenantId = 101;
    long workflowId99 = 99L;
    long workflowId100 = 99L;

    var lead = new LeadDetail();
    lead.setId(55L);
    lead.setFirstName("Johny");
    lead.setLastName("Stark");

    var old = new LeadDetail();
    old.setId(55L);
    old.setFirstName("Steve");
    old.setLastName("Roger");

    var metadata = mock(Metadata.class);
    var updatedMetadata = mock(Metadata.class);
    given(metadata.getTenantId()).willReturn(tenantId);
    given(metadata.getEntityType()).willReturn(LEAD);
    var updatedMetadataWithEntityId = mock(Metadata.class);
    given(updatedMetadataWithEntityId.getTenantId()).willReturn(tenantId);
    given(updatedMetadataWithEntityId.getEntityType()).willReturn(LEAD);

    given(metadata.with(workflowId99)).willReturn(updatedMetadata);
    given(metadata.isProcessed(workflowId99)).willReturn(true);
    given(updatedMetadata.withEntityId(55L)).willReturn(updatedMetadataWithEntityId);
    given(metadata.getEntityAction()).willReturn(EntityAction.UPDATED);

    var leadUpdatedEvent = new LeadEvent(lead, old, metadata);

    Workflow workflowMockUpdate =
        getMockEditPropertyWorkflow(workflowId100, TriggerFrequency.UPDATED, "firstName", "tony");

    List<Workflow> workflows = Arrays.asList(workflowMockUpdate);

    given(workflowService.findActiveBy(tenantId, LEAD, TriggerFrequency.UPDATED)).willReturn(workflows);
    doNothing().when(leadUpdatedCommandPublisher).execute(any(Metadata.class), any(Lead.class));
    // when
    workflowProcessor.process(leadUpdatedEvent);
    // then
    verifyNoInteractions(leadUpdatedCommandPublisher);
    verify(workflowService, times(0)).updateExecutedEventDetails(any(Workflow.class));
  }

  private Workflow getMockEditPropertyWorkflow(
      long workflowId, TriggerFrequency updated, String propertyName, String propertyValue) {

    Workflow workflowMock = mock(Workflow.class);
    given(workflowMock.getId()).willReturn(workflowId);

    EditPropertyAction workflowAction = mock(EditPropertyAction.class);
    given(workflowAction.getName()).willReturn(propertyName);
    given(workflowAction.getValue()).willReturn(propertyValue);
    given(workflowAction.getType()).willReturn(ActionType.EDIT_PROPERTY);
    Set<AbstractWorkflowAction> actions = new HashSet<>();
    actions.add(workflowAction);

    given(workflowMock.getWorkflowActions()).willReturn(actions);
    WorkflowTrigger workflowTriggerMock = mock(WorkflowTrigger.class);
    given(workflowTriggerMock.getTriggerType()).willReturn(TriggerType.EVENT);
    given(workflowTriggerMock.getTriggerFrequency()).willReturn(updated);
    given(workflowMock.getWorkflowTrigger()).willReturn(workflowTriggerMock);
    return workflowMock;
  }
}
