package com.kylas.sales.workflow.domain.processor;

import static com.kylas.sales.workflow.domain.workflow.EntityType.DEAL;
import static com.kylas.sales.workflow.domain.workflow.EntityType.LEAD;
import static com.kylas.sales.workflow.domain.workflow.TriggerFrequency.CREATED;
import static com.kylas.sales.workflow.domain.workflow.TriggerFrequency.UPDATED;
import static com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType.EDIT_PROPERTY;
import static com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType.REASSIGN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kylas.sales.workflow.api.WorkflowService;
import com.kylas.sales.workflow.api.request.Condition;
import com.kylas.sales.workflow.common.dto.condition.Operator;
import com.kylas.sales.workflow.common.dto.condition.WorkflowCondition.ConditionExpression;
import com.kylas.sales.workflow.domain.ConditionFacade;
import com.kylas.sales.workflow.domain.processor.deal.DealDetail;
import com.kylas.sales.workflow.domain.processor.deal.Money;
import com.kylas.sales.workflow.domain.processor.deal.Pipeline;
import com.kylas.sales.workflow.domain.processor.lead.IdName;
import com.kylas.sales.workflow.domain.processor.lead.Lead;
import com.kylas.sales.workflow.domain.processor.lead.LeadDetail;
import com.kylas.sales.workflow.domain.workflow.ConditionType;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.TriggerFrequency;
import com.kylas.sales.workflow.domain.workflow.TriggerType;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.WorkflowCondition;
import com.kylas.sales.workflow.domain.workflow.WorkflowTrigger;
import com.kylas.sales.workflow.domain.workflow.action.AbstractWorkflowAction;
import com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction;
import com.kylas.sales.workflow.domain.workflow.action.ValueConverter;
import com.kylas.sales.workflow.domain.workflow.action.reassign.ReassignAction;
import com.kylas.sales.workflow.domain.workflow.action.reassign.ReassignDetail;
import com.kylas.sales.workflow.mq.command.EntityUpdatedCommandPublisher;
import com.kylas.sales.workflow.mq.event.DealEvent;
import com.kylas.sales.workflow.mq.event.EntityAction;
import com.kylas.sales.workflow.mq.event.LeadEvent;
import com.kylas.sales.workflow.mq.event.Metadata;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Date;
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
  private EntityUpdatedCommandPublisher entityUpdatedCommandPublisher;
  @Mock
  private ValueConverter valueConverter;
  @InjectMocks
  private WorkflowProcessor workflowProcessor;
  @Mock
  private ConditionFacade conditionFacade;

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
        getMockEditPropertyWorkflow(workflowId, CREATED, "firstName", propertyValue);

    List<Workflow> workflows = Arrays.asList(workflowMock);

    given(workflowService.findActiveBy(tenantId, LEAD, CREATED)).willReturn(workflows);
    doNothing().when(entityUpdatedCommandPublisher).execute(any(Metadata.class), any(Lead.class));
    when(valueConverter.getValue(any(EditPropertyAction.class), any(Field.class), any(EntityType.class))).thenReturn(propertyValue);
    // when
    workflowProcessor.process(leadCreatedEvent);
    // then
    verify(entityUpdatedCommandPublisher, times(1))
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
    given(updateFirstNameAction.getType()).willReturn(EDIT_PROPERTY);
    actions.add(updateFirstNameAction);

    EditPropertyAction failedAction = mock(EditPropertyAction.class);
    given(failedAction.getName()).willReturn("firstName");
    given(failedAction.getValue()).willReturn(value);
    given(failedAction.getType()).willReturn(EDIT_PROPERTY);
    actions.add(failedAction);

    given(workflowMock.getWorkflowActions()).willReturn(actions);
    WorkflowTrigger workflowTriggerMock = mock(WorkflowTrigger.class);
    given(workflowTriggerMock.getTriggerType()).willReturn(TriggerType.EVENT);
    given(workflowTriggerMock.getTriggerFrequency()).willReturn(CREATED);
    given(workflowMock.getWorkflowTrigger()).willReturn(workflowTriggerMock);

    List<Workflow> workflows = Arrays.asList(workflowMock);

    given(workflowService.findActiveBy(tenantId, LEAD, CREATED)).willReturn(workflows);
    doNothing().when(entityUpdatedCommandPublisher).execute(any(Metadata.class), any(Lead.class));
    when(valueConverter.getValue(any(EditPropertyAction.class), any(Field.class), any(EntityType.class))).thenReturn(value);
    // when
    workflowProcessor.process(leadCreatedEvent);
    // then
    verify(entityUpdatedCommandPublisher, times(1))
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
        getMockEditPropertyWorkflow(workflowId100, UPDATED, "firstName", "tony");

    List<Workflow> workflows = Arrays.asList(workflowMockUpdate);

    given(workflowService.findActiveBy(tenantId, LEAD, UPDATED)).willReturn(workflows);
    doNothing().when(entityUpdatedCommandPublisher).execute(any(Metadata.class), any(Lead.class));
    when(valueConverter.getValue(any(EditPropertyAction.class), any(Field.class), any(EntityType.class))).thenReturn(value);
    // when
    workflowProcessor.process(leadUpdatedEvent);
    // then
    verify(entityUpdatedCommandPublisher, times(1))
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
        getMockEditPropertyWorkflow(workflowId100, UPDATED, "firstName", "tony");

    List<Workflow> workflows = Arrays.asList(workflowMockUpdate);

    given(workflowService.findActiveBy(tenantId, LEAD, UPDATED)).willReturn(workflows);
    doNothing().when(entityUpdatedCommandPublisher).execute(any(Metadata.class), any(Lead.class));
    // when
    workflowProcessor.process(leadUpdatedEvent);
    // then
    verifyNoInteractions(entityUpdatedCommandPublisher);
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
        getMockEditPropertyWorkflow(workflowId99, UPDATED, "firstName", "tony");

    Workflow workflowMockUpdate100 =
        getMockEditPropertyWorkflow(workflowId100, UPDATED, "lastName", "stark");

    List<Workflow> workflows = Arrays.asList(workflowMockUpdate99, workflowMockUpdate100);

    given(workflowService.findActiveBy(tenantId, LEAD, UPDATED)).willReturn(workflows);
    doNothing().when(entityUpdatedCommandPublisher).execute(any(Metadata.class), any(Lead.class));
    values.forEach(value -> {
      when(valueConverter.getValue(any(EditPropertyAction.class), any(Field.class), any(EntityType.class))).thenReturn(value);
    });
    // when
    workflowProcessor.process(leadUpdatedEvent);
    // then
    ArgumentCaptor<Metadata> metadataArgumentCaptor = ArgumentCaptor.forClass(Metadata.class);
    verify(entityUpdatedCommandPublisher, times(2)).execute(metadataArgumentCaptor.capture(), any(Lead.class));
    List<Metadata> allValues = metadataArgumentCaptor.getAllValues();
    Assertions.assertThat(allValues.size()).isEqualTo(2);
    Assertions.assertThat(allValues.get(0).getExecutedWorkflows()).containsExactlyInAnyOrder("WF_99", "WF_100");
    Assertions.assertThat(allValues.get(1).getExecutedWorkflows()).containsExactlyInAnyOrder("WF_99", "WF_100");
  }

  @Test
  public void givenLeadUpdatedEvent_shouldIncludeConditionSatisfyingWorkflowsOnly() {
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
    var condition = new WorkflowCondition(
        ConditionType.CONDITION_BASED,
        new ConditionExpression(Operator.EQUAL, "firstName", "rocky", Condition.TriggerType.NEW_VALUE));
    when(workflowMockUpdate100.getWorkflowCondition()).thenReturn(condition);
    when(conditionFacade.satisfies(any(), any())).thenReturn(false);

    List<Workflow> workflows = Arrays.asList(workflowMockUpdate99, workflowMockUpdate100);

    given(workflowService.findActiveBy(tenantId, LEAD, TriggerFrequency.UPDATED)).willReturn(workflows);
    doNothing().when(entityUpdatedCommandPublisher).execute(any(Metadata.class), any(Lead.class));
    values.forEach(value ->
        when(valueConverter.getValue(any(EditPropertyAction.class), any(Field.class), any(EntityType.class))).thenReturn(value));
    // when
    workflowProcessor.process(leadUpdatedEvent);
    // then
    ArgumentCaptor<Metadata> metadataArgumentCaptor = ArgumentCaptor.forClass(Metadata.class);
    verify(entityUpdatedCommandPublisher, times(1)).execute(metadataArgumentCaptor.capture(), any(Lead.class));
    List<Metadata> allValues = metadataArgumentCaptor.getAllValues();
    Assertions.assertThat(allValues.size()).isEqualTo(1);
    Assertions.assertThat(allValues.get(0).getExecutedWorkflows()).containsExactlyInAnyOrder("WF_99");
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
    given(workflowTriggerMock.getTriggerFrequency()).willReturn(CREATED);
    given(workflowMock.getWorkflowTrigger()).willReturn(workflowTriggerMock);

    List<Workflow> workflows = Arrays.asList(workflowMock);

    given(workflowService.findActiveBy(tenantId, LEAD, CREATED)).willReturn(workflows);
    doNothing().when(entityUpdatedCommandPublisher).execute(any(Metadata.class), any(Lead.class));
    // when
    workflowProcessor.process(leadCreatedEvent);
    // then
    verifyNoInteractions(entityUpdatedCommandPublisher);
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
    when(reassignAction.getType()).thenReturn(REASSIGN);

    Set<AbstractWorkflowAction> actions = new HashSet<>();
    actions.add(reassignAction);

    given(workflowMock.getWorkflowActions()).willReturn(actions);
    WorkflowTrigger workflowTriggerMock = mock(WorkflowTrigger.class);
    given(workflowTriggerMock.getTriggerType()).willReturn(TriggerType.EVENT);
    given(workflowTriggerMock.getTriggerFrequency()).willReturn(CREATED);
    given(workflowMock.getWorkflowTrigger()).willReturn(workflowTriggerMock);

    List<Workflow> workflows = Arrays.asList(workflowMock);

    given(workflowService.findActiveBy(tenantId, LEAD, CREATED)).willReturn(workflows);
    doNothing().when(entityUpdatedCommandPublisher).execute(any(Metadata.class), any(ReassignDetail.class));

    //when
    workflowProcessor.process(leadEvent);

    //Then
    verify(entityUpdatedCommandPublisher, times(1))
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
    given(workflowTriggerMock.getTriggerFrequency()).willReturn(CREATED);
    given(workflowMock.getWorkflowTrigger()).willReturn(workflowTriggerMock);

    List<Workflow> workflows = Arrays.asList(workflowMock);

    given(workflowService.findActiveBy(tenantId, LEAD, CREATED)).willReturn(workflows);
    doNothing().when(entityUpdatedCommandPublisher).execute(any(Metadata.class), any(ReassignDetail.class));

    //when
    workflowProcessor.process(leadEvent);

    //Then
    verifyNoInteractions(entityUpdatedCommandPublisher);
  }

  @Test
  public void givenDealEvent_withEditPropertyActions_shouldPublishEvent() {
    // given
    long tenantId = 101;
    long userId = 10L;
    long workflowId = 99L;

    var dealDetail = new DealDetail();
    dealDetail.setName("new deal");
    dealDetail.setOwnedBy(new IdName(1L, "James Bond"));
    dealDetail.setProduct(new IdName(2L, "CRM"));
    dealDetail.setCompany(new IdName(3L, "Dell enterprises"));
    dealDetail.setPipeline(new Pipeline(1L, "test", new IdName(2L, "Open")));
    dealDetail.setEstimatedClosureOn(new Date());
    dealDetail.setEstimatedValue(new Money(1L, 3000d));
    dealDetail.setAssociatedContacts(List.of(new IdName(2L, "Tony")));
    dealDetail.setId(55L);
    Metadata metadata = new Metadata(tenantId, userId, DEAL, null, null, EntityAction.CREATED);

    var dealEvent = new DealEvent(dealDetail, null, metadata);

    Workflow workflowMock = mock(Workflow.class);
    given(workflowMock.getId()).willReturn(workflowId);

    Set<AbstractWorkflowAction> actions = new HashSet<>();

    EditPropertyAction updateDealNameAction = mock(EditPropertyAction.class);
    given(updateDealNameAction.getName()).willReturn("name");
    given(updateDealNameAction.getValue()).willReturn(dealDetail.getName());
    given(updateDealNameAction.getType()).willReturn(EDIT_PROPERTY);
    actions.add(updateDealNameAction);

    EditPropertyAction updateOwnedBy = mock(EditPropertyAction.class);
    given(updateOwnedBy.getName()).willReturn("ownedBy");
    given(updateOwnedBy.getValue()).willReturn(dealDetail.getOwnedBy());
    given(updateOwnedBy.getType()).willReturn(EDIT_PROPERTY);
    actions.add(updateOwnedBy);

    EditPropertyAction updateProduct = mock(EditPropertyAction.class);
    given(updateProduct.getName()).willReturn("product");
    given(updateProduct.getValue()).willReturn(dealDetail.getProduct());
    given(updateProduct.getType()).willReturn(EDIT_PROPERTY);
    actions.add(updateProduct);

    EditPropertyAction updateCompany = mock(EditPropertyAction.class);
    given(updateCompany.getName()).willReturn("company");
    given(updateCompany.getValue()).willReturn(dealDetail.getCompany());
    given(updateCompany.getType()).willReturn(EDIT_PROPERTY);
    actions.add(updateCompany);

    EditPropertyAction updatePipeline = mock(EditPropertyAction.class);
    given(updatePipeline.getName()).willReturn("pipeline");
    given(updatePipeline.getValue()).willReturn(dealDetail.getPipeline());
    given(updatePipeline.getType()).willReturn(EDIT_PROPERTY);
    actions.add(updatePipeline);

    EditPropertyAction updateEstimatedClosureOn = mock(EditPropertyAction.class);
    given(updateEstimatedClosureOn.getName()).willReturn("estimatedClosureOn");
    given(updateEstimatedClosureOn.getValue()).willReturn(dealDetail.getEstimatedClosureOn());
    given(updateEstimatedClosureOn.getType()).willReturn(EDIT_PROPERTY);
    actions.add(updateEstimatedClosureOn);

    EditPropertyAction updateEstimatedValue = mock(EditPropertyAction.class);
    given(updateEstimatedValue.getName()).willReturn("estimatedValue");
    given(updateEstimatedValue.getValue()).willReturn(dealDetail.getEstimatedValue());
    given(updateEstimatedValue.getType()).willReturn(EDIT_PROPERTY);
    actions.add(updateEstimatedValue);

    EditPropertyAction updateAssociatedContacts = mock(EditPropertyAction.class);
    given(updateAssociatedContacts.getName()).willReturn("associatedContacts");
    given(updateAssociatedContacts.getValue()).willReturn(dealDetail.getAssociatedContacts());
    given(updateAssociatedContacts.getType()).willReturn(EDIT_PROPERTY);
    actions.add(updateAssociatedContacts);

    given(workflowMock.getWorkflowActions()).willReturn(actions);
    WorkflowTrigger workflowTriggerMock = mock(WorkflowTrigger.class);
    given(workflowTriggerMock.getTriggerType()).willReturn(TriggerType.EVENT);
    given(workflowTriggerMock.getTriggerFrequency()).willReturn(CREATED);
    given(workflowMock.getWorkflowTrigger()).willReturn(workflowTriggerMock);

    List<Workflow> workflows = Arrays.asList(workflowMock);

    given(workflowService.findActiveBy(tenantId, DEAL, CREATED)).willReturn(workflows);
    doNothing().when(entityUpdatedCommandPublisher).execute(any(Metadata.class), any(DealDetail.class));
    actions.forEach(action -> {
      EditPropertyAction editPropertyAction = (EditPropertyAction) action;
      Object value = editPropertyAction.getValue();
      String name = editPropertyAction.getName();
      try {
        when(valueConverter.getValue(editPropertyAction, dealDetail.getClass().getDeclaredField(name), DEAL)).thenReturn(value);
      } catch (NoSuchFieldException e) {
        log.error(e.getMessage());
      }
    });
    // when
    workflowProcessor.process(dealEvent);
    // then
    verify(entityUpdatedCommandPublisher, times(1))
        .execute(any(Metadata.class), any(Actionable.class));
  }

  @Test
  public void givenDealEvent_withoutEditPropertyActions_shouldNotPublishEvent() {
    // given
    long tenantId = 101;
    long userId = 10L;
    long workflowId = 99L;

    var dealDetail = new DealDetail();
    dealDetail.setName("new deal");
    dealDetail.setOwnedBy(new IdName(1L, "James Bond"));
    dealDetail.setProduct(new IdName(2L, "CRM"));
    dealDetail.setCompany(new IdName(3L, "Dell enterprises"));
    dealDetail.setPipeline(new Pipeline(1L, "test", new IdName(2L, "Open")));
    dealDetail.setEstimatedClosureOn(new Date());
    dealDetail.setEstimatedValue(new Money(1L, 3000d));
    dealDetail.setAssociatedContacts(List.of(new IdName(2L, "Tony")));
    dealDetail.setId(55L);
    Metadata metadata = new Metadata(tenantId, userId, DEAL, null, null, EntityAction.CREATED);

    var dealEvent = new DealEvent(dealDetail, null, metadata);

    Workflow workflowMock = mock(Workflow.class);
    given(workflowMock.getId()).willReturn(workflowId);

    Set<AbstractWorkflowAction> actions = new HashSet<>();

    List<Workflow> workflows = Arrays.asList(workflowMock);

    given(workflowService.findActiveBy(tenantId, DEAL, CREATED)).willReturn(workflows);
    doNothing().when(entityUpdatedCommandPublisher).execute(any(Metadata.class), any(DealDetail.class));
    // when
    workflowProcessor.process(dealEvent);
    // then
    verifyNoInteractions(entityUpdatedCommandPublisher);
  }


  private Workflow getMockEditPropertyWorkflow(
      long workflowId, TriggerFrequency updated, String propertyName, String propertyValue) {

    Workflow workflowMock = mock(Workflow.class);
    given(workflowMock.getId()).willReturn(workflowId);

    EditPropertyAction workflowAction = mock(EditPropertyAction.class);
    given(workflowAction.getName()).willReturn(propertyName);
    given(workflowAction.getValue()).willReturn(propertyValue);
    given(workflowAction.getType()).willReturn(EDIT_PROPERTY);
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
