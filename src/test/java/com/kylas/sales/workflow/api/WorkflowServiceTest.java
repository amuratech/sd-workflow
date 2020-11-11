package com.kylas.sales.workflow.api;

import static com.kylas.sales.workflow.domain.workflow.EntityType.LEAD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import com.kylas.sales.workflow.api.request.WorkflowRequest;
import com.kylas.sales.workflow.api.response.WorkflowDetail;
import com.kylas.sales.workflow.api.response.WorkflowSummary;
import com.kylas.sales.workflow.common.dto.WorkflowAction;
import com.kylas.sales.workflow.domain.WorkflowFacade;
import com.kylas.sales.workflow.domain.user.User;
import com.kylas.sales.workflow.domain.workflow.ConditionType;
import com.kylas.sales.workflow.domain.workflow.TriggerFrequency;
import com.kylas.sales.workflow.domain.workflow.TriggerType;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.WorkflowCondition;
import com.kylas.sales.workflow.domain.workflow.WorkflowExecutedEvent;
import com.kylas.sales.workflow.domain.workflow.WorkflowTrigger;
import com.kylas.sales.workflow.domain.workflow.action.AbstractWorkflowAction;
import com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType;
import com.kylas.sales.workflow.matchers.PageableMatcher;
import com.kylas.sales.workflow.stubs.UserStub;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

  @InjectMocks
  private WorkflowService workflowService;

  @Mock
  private WorkflowFacade workflowFacade;

  @Test
  public void givenWorkflowRequest_shouldCreateIt() {
    //given
    var workflowRequestMock = mock(WorkflowRequest.class);
    var workflowMock = mock(Workflow.class);
    given(workflowMock.getId()).willReturn(1L);
    given(workflowFacade.create(workflowRequestMock)).willReturn(Mono.just(workflowMock));
    //when
    Mono<WorkflowSummary> workflowSummaryMono = workflowService.create(workflowRequestMock);
    //then
    StepVerifier.create(workflowSummaryMono)
        .assertNext(workflowSummary -> assertThat(workflowMock.getId()).isEqualTo(1L))
        .verifyComplete();
  }

  @Test
  public void givenWorkflowUpdateRequest_shouldUpdateIt() {
    //given
    var workflowRequestMock = mock(WorkflowRequest.class);
    var workflowMock = mock(Workflow.class);
    given(workflowMock.getId()).willReturn(1L);
    given(workflowFacade.create(workflowRequestMock)).willReturn(Mono.just(workflowMock));
    //when
    Mono<WorkflowSummary> workflowSummaryMono = workflowService.create(workflowRequestMock);
    //then
    StepVerifier.create(workflowSummaryMono)
        .assertNext(workflowSummary -> assertThat(workflowMock.getId()).isEqualTo(1L))
        .verifyComplete();
  }

  @Test
  public void givenTenantAndEntityType_shouldReturnWorkflows() {
    //given
    long tenantId = 99;
    given(workflowFacade.findActiveBy(tenantId, LEAD)).willReturn(Arrays.asList(mock(Workflow.class), mock(Workflow.class)));
    //when
    List<Workflow> workflows = workflowService.findActiveBy(tenantId, LEAD);
    //then
    assertThat(workflows.size()).isEqualTo(2);

  }

  @Test
  public void givenWorkflowId_shouldGetWorkflowDetail() {
    //given
    long workflowId = 88L;
    User aUser = UserStub.aUser(11L, 99L, true, true, true, false, false)
        .withName("user 1");
    WorkflowTrigger trigger = WorkflowTrigger
        .createNew(new com.kylas.sales.workflow.common.dto.WorkflowTrigger(TriggerType.EVENT, TriggerFrequency.CREATED));
    WorkflowCondition condition = WorkflowCondition.createNew(new com.kylas.sales.workflow.common.dto.WorkflowCondition(ConditionType.FOR_ALL));
    Set<AbstractWorkflowAction> actions = new HashSet<>();
    EditPropertyAction editPropertyAction = new EditPropertyAction();
    UUID id = UUID.randomUUID();
    editPropertyAction.setId(id);
    editPropertyAction.setName("firstName");
    editPropertyAction.setValue("tony");
    actions.add(editPropertyAction);

    Workflow workflow = Workflow.createNew("Workflow 1", "Workflow 1", LEAD, trigger, aUser, actions, condition, true);
    Workflow workflowSpy = Mockito.spy(workflow);
    given(workflowSpy.getId()).willReturn(workflowId);
    given(workflowFacade.get(workflowId)).willReturn(workflowSpy);
    //when
    WorkflowDetail workflowDetail = workflowService.get(workflowId);
    //then
    assertThat(workflowDetail.getId()).isEqualTo(workflowId);
    assertThat(workflowDetail.getName()).isEqualTo("Workflow 1");
    assertThat(workflowDetail.getDescription()).isEqualTo("Workflow 1");
    assertThat(workflowDetail.getEntityType()).isEqualTo(LEAD);

    assertThat(workflowDetail.getTrigger().getName()).isEqualTo(TriggerType.EVENT);
    assertThat(workflowDetail.getTrigger().getTriggerFrequency()).isEqualTo(TriggerFrequency.CREATED);

    assertThat(workflowDetail.getActions().size()).isEqualTo(1);
    WorkflowAction workflowActionResponse = workflowDetail.getActions().iterator().next();
    assertThat(workflowActionResponse.getType()).isEqualTo(ActionType.EDIT_PROPERTY);
    assertThat(workflowActionResponse.getPayload().getName()).isEqualTo("firstName");
    assertThat(workflowActionResponse.getPayload().getValue()).isEqualTo("tony");
  }

  @Test
  public void givenWorkflowId_shouldActivateWorkflow() {
    //given
    long workflowId = 1000L;
    Workflow workflow = aWorkflowActivationStub(true);
    given(workflowFacade.activate(workflowId)).willReturn(workflow);
    //when
    WorkflowDetail activatedWorkflow = workflowService.activate(workflowId);
    //then
    assertThat(activatedWorkflow.isActive()).isTrue();
    assertThat(activatedWorkflow.getId()).isEqualTo(1000L);
    assertThat(activatedWorkflow.getName()).isEqualTo("Workflow on bills");
  }

  @NotNull
  private Workflow aWorkflowActivationStub(boolean active) {
    Workflow workflow = new Workflow();
    workflow.setId(1000L);
    workflow.setActive(active);
    workflow.setName("Workflow on bills");
    workflow.setWorkflowTrigger(
        WorkflowTrigger.createNew(new com.kylas.sales.workflow.common.dto.WorkflowTrigger(TriggerType.EVENT, TriggerFrequency.CREATED)));
    workflow.setWorkflowCondition(WorkflowCondition.createNew(new com.kylas.sales.workflow.common.dto.WorkflowCondition(ConditionType.FOR_ALL)));
    workflow.setWorkflowActions(Collections.emptySet());
    workflow.setCreatedBy(new User(4000L, 1000L, Collections.emptySet()));
    workflow.setUpdatedBy(new User(4000L, 1000L, Collections.emptySet()));
    workflow.setWorkflowCondition(new WorkflowCondition());
    var workflowExecutedEvent = new WorkflowExecutedEvent();
    workflowExecutedEvent.setLastTriggeredAt(new Date());
    workflowExecutedEvent.setTriggerCount(30L);
    workflow.setWorkflowExecutedEvent(workflowExecutedEvent);
    return workflow;
  }

  @Test
  public void givenWorkflowId_shouldDeactivateWorkflow() {
    //given
    long workflowId = 1000L;
    Workflow workflow = aWorkflowActivationStub(false);
    given(workflowFacade.deactivate(workflowId)).willReturn(workflow);
    //when
    WorkflowDetail activatedWorkflow = workflowService.deactivate(workflowId);
    //then
    assertThat(activatedWorkflow.isActive()).isFalse();
    assertThat(activatedWorkflow.getId()).isEqualTo(1000L);
    assertThat(activatedWorkflow.getName()).isEqualTo("Workflow on bills");
  }

  @Test
  public void shouldGetWorkflowDetailList() {
    //given
    User aUser = UserStub.aUser(11L, 99L, true, true, true, false, false)
        .withName("user 1");
    WorkflowTrigger trigger = WorkflowTrigger
        .createNew(new com.kylas.sales.workflow.common.dto.WorkflowTrigger(TriggerType.EVENT, TriggerFrequency.CREATED));
    WorkflowCondition condition = WorkflowCondition.createNew(new com.kylas.sales.workflow.common.dto.WorkflowCondition(ConditionType.FOR_ALL));
    Set<AbstractWorkflowAction> actions = new HashSet<>();
    EditPropertyAction editPropertyAction = new EditPropertyAction();
    UUID id = UUID.randomUUID();
    editPropertyAction.setId(id);
    editPropertyAction.setName("firstName");
    editPropertyAction.setValue("tony");
    actions.add(editPropertyAction);

    Workflow workflow = Workflow.createNew("Workflow 1", "Workflow 1", LEAD, trigger, aUser, actions, condition, true);
    workflow.setAllowedActionsForUser(aUser);
    Workflow workflowSpy = Mockito.spy(workflow);
    given(workflowSpy.getId()).willReturn(100L);
    List<Workflow> workflows = new ArrayList<>();
    workflows.add(workflowSpy);

    PageImpl<Workflow> workflowPageable = new PageImpl<>(workflows, PageRequest.of(0, 10), 12);

    given(workflowFacade.list(argThat(new PageableMatcher(0, 10, Sort.unsorted())))).willReturn(workflowPageable);
    PageRequest pageable = PageRequest.of(0, 10);
    //when
    Mono<Page<WorkflowDetail>> workflowPages = workflowService.list(pageable);
    //then
    StepVerifier.create(workflowPages)
        .assertNext(workflowDetails -> {
          assertThat(workflowDetails.getTotalElements()).isEqualTo(12);
          assertThat(workflowDetails.getNumber()).isEqualTo(0);
          assertThat(workflowDetails.getTotalPages()).isEqualTo(2);
          assertThat(workflowDetails.isFirst()).isTrue();
          assertThat(workflowDetails.isLast()).isFalse();

          assertThat(workflowDetails.getContent().size()).isEqualTo(1);

          WorkflowDetail workflowDetail = workflowDetails.getContent().get(0);
          assertThat(workflowDetail.getId()).isEqualTo(100L);
          assertThat(workflowDetail.getName()).isEqualTo("Workflow 1");
          assertThat(workflowDetail.getDescription()).isEqualTo("Workflow 1");
          assertThat(workflowDetail.getEntityType()).isEqualTo(LEAD);

          assertThat(workflowDetail.getTrigger().getName()).isEqualTo(TriggerType.EVENT);
          assertThat(workflowDetail.getTrigger().getTriggerFrequency()).isEqualTo(TriggerFrequency.CREATED);

          assertThat(workflowDetail.getActions().size()).isEqualTo(1);
          WorkflowAction workflowActionResponse = workflowDetail.getActions().iterator().next();
          assertThat(workflowActionResponse.getType()).isEqualTo(ActionType.EDIT_PROPERTY);
          assertThat(workflowActionResponse.getPayload().getName()).isEqualTo("firstName");
          assertThat(workflowActionResponse.getPayload().getValue()).isEqualTo("tony");

          assertThat(workflowDetail.getAllowedActions().canRead()).isTrue();

        }).verifyComplete();
  }

  @Test
  public void givenWorkflow_shouldUpdateExecutedEvent(){
    //given
    User aUser = UserStub.aUser(11L, 99L, true, true, true, false, false)
        .withName("user 1");
    WorkflowTrigger trigger = WorkflowTrigger
        .createNew(new com.kylas.sales.workflow.common.dto.WorkflowTrigger(TriggerType.EVENT, TriggerFrequency.CREATED));
    WorkflowCondition condition = WorkflowCondition.createNew(new com.kylas.sales.workflow.common.dto.WorkflowCondition(ConditionType.FOR_ALL));
    Set<AbstractWorkflowAction> actions = new HashSet<>();
    EditPropertyAction editPropertyAction = new EditPropertyAction();
    UUID id = UUID.randomUUID();
    editPropertyAction.setId(id);
    editPropertyAction.setName("firstName");
    editPropertyAction.setValue("tony");
    actions.add(editPropertyAction);

    Workflow workflow = Workflow.createNew("Workflow 1", "Workflow 1", LEAD, trigger, aUser, actions, condition, true);
    BDDMockito.doNothing().when(workflowFacade).updateExecutedEvent(workflow);
    //when
    workflowService.updateExecutedEventDetails(workflow);
    //then
    Mockito.verify(workflowFacade, times(1)).updateExecutedEvent(any(Workflow.class));

  }
}