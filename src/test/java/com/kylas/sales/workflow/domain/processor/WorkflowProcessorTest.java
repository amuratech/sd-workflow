package com.kylas.sales.workflow.domain.processor;

import static com.kylas.sales.workflow.domain.workflow.EntityType.LEAD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kylas.sales.workflow.api.WorkflowService;
import com.kylas.sales.workflow.domain.processor.lead.Lead;
import com.kylas.sales.workflow.domain.processor.lead.LeadDetail;
import com.kylas.sales.workflow.domain.workflow.TriggerFrequency;
import com.kylas.sales.workflow.domain.workflow.TriggerType;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.WorkflowTrigger;
import com.kylas.sales.workflow.domain.workflow.action.AbstractWorkflowAction;
import com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction;
import com.kylas.sales.workflow.domain.workflow.action.ValueConverter;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType;
import com.kylas.sales.workflow.domain.workflow.action.reassign.ReassignAction;
import com.kylas.sales.workflow.domain.workflow.action.reassign.ReassignDetail;
import com.kylas.sales.workflow.mq.command.LeadUpdatedCommandPublisher;
import com.kylas.sales.workflow.mq.event.EntityAction;
import com.kylas.sales.workflow.mq.event.LeadEvent;
import com.kylas.sales.workflow.mq.event.Metadata;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@Slf4j
class WorkflowProcessorTest {

  @Mock
  private WorkflowService workflowService;
  @Mock
  private LeadUpdatedCommandPublisher leadUpdatedCommandPublisher;
  @Mock
  private ValueConverter valueConverter;
  @InjectMocks
  private WorkflowProcessor workflowProcessor;

  @Test
  public void givenLeadEvent_shouldPublishPatchCommand() {
    // given
    long tenantId = 101;
    long userId = 10L;
    long workflowId = 99L;

    var lead = new LeadDetail();
    lead.setId(55L);
    lead.setFirstName("Tony");
    lead.setLastName("Stark");
    Metadata metadata = new Metadata(tenantId, userId, LEAD, null, null, EntityAction.CREATED);

    var leadCreatedEvent = new LeadEvent(lead, null, metadata);

    String propertyValue = "steve";

    Workflow workflowMock =
        getMockEditPropertyWorkflow(workflowId, TriggerFrequency.CREATED, "firstName", propertyValue);

    List<Workflow> workflows = Arrays.asList(workflowMock);

    given(workflowService.findActiveBy(tenantId, LEAD, TriggerFrequency.CREATED)).willReturn(workflows);
    doNothing().when(leadUpdatedCommandPublisher).execute(any(Metadata.class), any(Lead.class));
    when(valueConverter.getValue(any(EditPropertyAction.class), any(Field.class))).thenReturn(propertyValue);
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
    long userId = 10L;
    long workflowId = 99L;

    var lead = new LeadDetail();
    lead.setFirstName("Tony");
    lead.setLastName("Stark");
    lead.setId(55L);
    Metadata metadata = new Metadata(tenantId, userId, LEAD, null, null, EntityAction.CREATED);

    var leadCreatedEvent = new LeadEvent(lead, null, metadata);

    Workflow workflowMock = mock(Workflow.class);
    given(workflowMock.getId()).willReturn(workflowId);

    Set<AbstractWorkflowAction> actions = new HashSet<>();

    String value = "steve";

    EditPropertyAction updateFirstNameAction = mock(EditPropertyAction.class);
    given(updateFirstNameAction.getName()).willReturn("firstName");
    given(updateFirstNameAction.getValue()).willReturn(value);
    given(updateFirstNameAction.getType()).willReturn(ActionType.EDIT_PROPERTY);
    actions.add(updateFirstNameAction);

    EditPropertyAction failedAction = mock(EditPropertyAction.class);
    given(failedAction.getName()).willReturn("firstName");
    given(failedAction.getValue()).willReturn(value);
    given(failedAction.getType()).willReturn(ActionType.EDIT_PROPERTY);
    actions.add(failedAction);

    given(workflowMock.getWorkflowActions()).willReturn(actions);
    WorkflowTrigger workflowTriggerMock = mock(WorkflowTrigger.class);
    given(workflowTriggerMock.getTriggerType()).willReturn(TriggerType.EVENT);
    given(workflowTriggerMock.getTriggerFrequency()).willReturn(TriggerFrequency.CREATED);
    given(workflowMock.getWorkflowTrigger()).willReturn(workflowTriggerMock);

    List<Workflow> workflows = Arrays.asList(workflowMock);

    given(workflowService.findActiveBy(tenantId, LEAD, TriggerFrequency.CREATED)).willReturn(workflows);
    doNothing().when(leadUpdatedCommandPublisher).execute(any(Metadata.class), any(Lead.class));
    when(valueConverter.getValue(any(EditPropertyAction.class), any(Field.class))).thenReturn(value);
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
    long userId = 10L;
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

    var metadata = new Metadata(tenantId, userId, LEAD, null, null, EntityAction.UPDATED);
    var leadUpdatedEvent = new LeadEvent(lead, old, metadata);

    String value = "tony";

    Workflow workflowMockUpdate =
        getMockEditPropertyWorkflow(workflowId100, TriggerFrequency.UPDATED, "firstName", "tony");

    List<Workflow> workflows = Arrays.asList(workflowMockUpdate);

    given(workflowService.findActiveBy(tenantId, LEAD, TriggerFrequency.UPDATED)).willReturn(workflows);
    doNothing().when(leadUpdatedCommandPublisher).execute(any(Metadata.class), any(Lead.class));
    when(valueConverter.getValue(any(EditPropertyAction.class), any(Field.class))).thenReturn(value);
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
    long userId = 102;
    long workflowId100 = 100L;

    var lead = new LeadDetail();
    lead.setId(55L);
    lead.setFirstName("Johny");
    lead.setLastName("Stark");

    var old = new LeadDetail();
    old.setId(55L);
    old.setFirstName("Steve");
    old.setLastName("Roger");

    Set<String> executedWorkflows = new HashSet<>();
    executedWorkflows.add("WF_100");
    var metadata = new Metadata(tenantId,userId,LEAD,"WF_99",executedWorkflows,EntityAction.UPDATED);
    var updatedMetadata = mock(Metadata.class);
    var updatedMetadataWithEntityId = mock(Metadata.class);
    given(updatedMetadataWithEntityId.getTenantId()).willReturn(tenantId);
    given(updatedMetadataWithEntityId.getEntityType()).willReturn(LEAD);

    given(updatedMetadata.withEntityId(55L)).willReturn(updatedMetadataWithEntityId);

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

  @Test
  public void givenLeadUpdatedEvent_tryExecuteMultipleWorkflowWithSameCondition_shouldSendAllWorkflowIdsAsExcuted() {
    // given
    long tenantId = 101;
    long userId = 102;
    long workflowId99 = 99L;
    long workflowId100 = 100L;

    var lead = new LeadDetail();
    lead.setId(55L);
    lead.setFirstName("Johny");
    lead.setLastName("Stark");

    var old = new LeadDetail();
    old.setId(55L);
    old.setFirstName("Steve");
    old.setLastName("Roger");

    var metadata = new Metadata(tenantId, userId, LEAD, null, null, EntityAction.UPDATED);

    var leadUpdatedEvent = new LeadEvent(lead, old, metadata);

    List<String> values = List.of("tony", "steve");

    Workflow workflowMockUpdate99 =
        getMockEditPropertyWorkflow(workflowId99, TriggerFrequency.UPDATED, "firstName", "tony");

    Workflow workflowMockUpdate100 =
        getMockEditPropertyWorkflow(workflowId100, TriggerFrequency.UPDATED, "lastName", "stark");

    List<Workflow> workflows = Arrays.asList(workflowMockUpdate99, workflowMockUpdate100);

    given(workflowService.findActiveBy(tenantId, LEAD, TriggerFrequency.UPDATED)).willReturn(workflows);
    doNothing().when(leadUpdatedCommandPublisher).execute(any(Metadata.class), any(Lead.class));
    values.forEach(value -> {
      when(valueConverter.getValue(any(EditPropertyAction.class), any(Field.class))).thenReturn(value);
    });
    // when
    workflowProcessor.process(leadUpdatedEvent);
    // then
    ArgumentCaptor<Metadata> metadataArgumentCaptor = ArgumentCaptor.forClass(Metadata.class);
    verify(leadUpdatedCommandPublisher, times(2)).execute(metadataArgumentCaptor.capture(), any(Lead.class));
    List<Metadata> allValues = metadataArgumentCaptor.getAllValues();
    Assertions.assertThat(allValues.size()).isEqualTo(2);
    Assertions.assertThat(allValues.get(0).getExecutedWorkflows()).containsExactlyInAnyOrder("WF_99", "WF_100");
    Assertions.assertThat(allValues.get(1).getExecutedWorkflows()).containsExactlyInAnyOrder("WF_99", "WF_100");
  }

  @Test
  public void givenLeadEvent_withoutEditPropertyActions_shouldNotPublishEvent() {
    // given
    long tenantId = 101;
    long userId = 10L;
    long workflowId = 99L;

    var lead = new LeadDetail();
    lead.setFirstName("Tony");
    lead.setLastName("Stark");
    lead.setId(55L);
    Metadata metadata = new Metadata(tenantId, userId, LEAD, null, null, EntityAction.CREATED);

    var leadCreatedEvent = new LeadEvent(lead, null, metadata);

    Workflow workflowMock = mock(Workflow.class);
    given(workflowMock.getId()).willReturn(workflowId);

    Set<AbstractWorkflowAction> actions = new HashSet<>();

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
    verifyNoInteractions(leadUpdatedCommandPublisher);
  }

  @Test
  public void givenLeadEvent_withReassignActions_shouldPublishEvent() {
    // given
    long tenantId = 101;
    long userId = 10L;
    long workflowId = 99L;

    var lead = new LeadDetail();
    lead.setFirstName("Tony");
    lead.setLastName("Stark");
    lead.setId(55L);
    Metadata metadata = new Metadata(tenantId, userId, LEAD, null, null, EntityAction.CREATED);

    var leadEvent = new LeadEvent(lead, null, metadata);

    Workflow workflowMock = mock(Workflow.class);
    given(workflowMock.getId()).willReturn(workflowId);

    ReassignAction reassignAction = mock(ReassignAction.class);
    when(reassignAction.getOwnerId()).thenReturn(2000L);
    when(reassignAction.getType()).thenReturn(ActionType.REASSIGN);

    Set<AbstractWorkflowAction> actions = new HashSet<>();
    actions.add(reassignAction);

    given(workflowMock.getWorkflowActions()).willReturn(actions);
    WorkflowTrigger workflowTriggerMock = mock(WorkflowTrigger.class);
    given(workflowTriggerMock.getTriggerType()).willReturn(TriggerType.EVENT);
    given(workflowTriggerMock.getTriggerFrequency()).willReturn(TriggerFrequency.CREATED);
    given(workflowMock.getWorkflowTrigger()).willReturn(workflowTriggerMock);

    List<Workflow> workflows = Arrays.asList(workflowMock);

    given(workflowService.findActiveBy(tenantId, LEAD, TriggerFrequency.CREATED)).willReturn(workflows);
    doNothing().when(leadUpdatedCommandPublisher).execute(any(Metadata.class), any(ReassignDetail.class));

    //when
    workflowProcessor.process(leadEvent);

    //Then
    verify(leadUpdatedCommandPublisher, times(1))
        .execute(any(Metadata.class), any(Actionable.class));
  }


  @Test
  public void givenLeadEvent_withoutReassignActions_shouldNotPublishEvent() {
    // given
    long tenantId = 101;
    long userId = 10L;
    long workflowId = 99L;

    var lead = new LeadDetail();
    lead.setFirstName("Tony");
    lead.setLastName("Stark");
    lead.setId(55L);
    Metadata metadata = new Metadata(tenantId, userId, LEAD, null, null, EntityAction.CREATED);

    var leadEvent = new LeadEvent(lead, null, metadata);

    Workflow workflowMock = mock(Workflow.class);
    given(workflowMock.getId()).willReturn(workflowId);

    Set<AbstractWorkflowAction> actions = new HashSet<>();

    given(workflowMock.getWorkflowActions()).willReturn(actions);
    WorkflowTrigger workflowTriggerMock = mock(WorkflowTrigger.class);
    given(workflowTriggerMock.getTriggerType()).willReturn(TriggerType.EVENT);
    given(workflowTriggerMock.getTriggerFrequency()).willReturn(TriggerFrequency.CREATED);
    given(workflowMock.getWorkflowTrigger()).willReturn(workflowTriggerMock);

    List<Workflow> workflows = Arrays.asList(workflowMock);

    given(workflowService.findActiveBy(tenantId, LEAD, TriggerFrequency.CREATED)).willReturn(workflows);
    doNothing().when(leadUpdatedCommandPublisher).execute(any(Metadata.class), any(ReassignDetail.class));

    //when
    workflowProcessor.process(leadEvent);

    //Then
    verifyNoInteractions(leadUpdatedCommandPublisher);
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
