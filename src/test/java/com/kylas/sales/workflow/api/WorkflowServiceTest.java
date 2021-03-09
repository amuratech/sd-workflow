package com.kylas.sales.workflow.api;

import static com.kylas.sales.workflow.domain.workflow.ConditionType.FOR_ALL;
import static com.kylas.sales.workflow.domain.workflow.EntityType.CONTACT;
import static com.kylas.sales.workflow.domain.workflow.EntityType.DEAL;
import static com.kylas.sales.workflow.domain.workflow.EntityType.LEAD;
import static com.kylas.sales.workflow.domain.workflow.TriggerFrequency.CREATED;
import static com.kylas.sales.workflow.domain.workflow.TriggerType.EVENT;
import static com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType.CREATE_TASK;
import static com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType.EDIT_PROPERTY;
import static com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType.SEND_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.kylas.sales.workflow.api.request.FilterRequest;
import com.kylas.sales.workflow.api.request.FilterRequest.Filter;
import com.kylas.sales.workflow.api.request.WorkflowRequest;
import com.kylas.sales.workflow.api.response.WorkflowDetail;
import com.kylas.sales.workflow.api.response.WorkflowEntry;
import com.kylas.sales.workflow.api.response.WorkflowSummary;
import com.kylas.sales.workflow.common.dto.ActionDetail;
import com.kylas.sales.workflow.common.dto.ActionDetail.CreateTaskAction;
import com.kylas.sales.workflow.common.dto.ActionDetail.EmailAction;
import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.domain.WorkflowFacade;
import com.kylas.sales.workflow.domain.processor.task.AssignedToType;
import com.kylas.sales.workflow.domain.service.ValueResolver;
import com.kylas.sales.workflow.domain.user.User;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.WorkflowCondition;
import com.kylas.sales.workflow.domain.workflow.WorkflowExecutedEvent;
import com.kylas.sales.workflow.domain.workflow.WorkflowTrigger;
import com.kylas.sales.workflow.domain.workflow.action.AbstractWorkflowAction;
import com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType;
import com.kylas.sales.workflow.domain.workflow.action.email.EmailActionType;
import com.kylas.sales.workflow.domain.workflow.action.email.EmailEntityType;
import com.kylas.sales.workflow.domain.workflow.action.email.Participant;
import com.kylas.sales.workflow.domain.workflow.action.task.AssignedTo;
import com.kylas.sales.workflow.domain.workflow.action.task.DueDate;
import com.kylas.sales.workflow.matchers.PageableMatcher;
import com.kylas.sales.workflow.security.AuthService;
import com.kylas.sales.workflow.stubs.UserStub;
import com.kylas.sales.workflow.stubs.WorkflowStub;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
import org.springframework.data.domain.Sort.Order;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

  @InjectMocks
  private WorkflowService workflowService;

  @Mock
  private WorkflowFacade workflowFacade;

  @Mock
  private AuthService authService;

  @Mock
  private ValueResolver valueResolver;

  @Test
  public void givenWorkflowRequest_shouldCreateIt() {
    // given
    var workflowRequestMock = mock(WorkflowRequest.class);
    var workflowMock = mock(Workflow.class);
    given(workflowMock.getId()).willReturn(1L);
    given(workflowFacade.create(workflowRequestMock)).willReturn(Mono.just(workflowMock));
    doNothing().when(workflowFacade).validate(workflowRequestMock);
    // when
    Mono<WorkflowSummary> workflowSummaryMono = workflowService.create(workflowRequestMock);
    // then
    verify(workflowFacade, times(1)).validate(workflowRequestMock);
    StepVerifier.create(workflowSummaryMono)
        .assertNext(workflowSummary -> assertThat(workflowMock.getId()).isEqualTo(1L))
        .verifyComplete();
  }

  @Test
  public void givenLeadWorkflowRequest_withCreateTaskAction_shouldCreateIt() {
    // given
    var actions = Set.of(new ActionResponse(CREATE_TASK,
        new CreateTaskAction("new Task", "new Task description", 1L, "contacted", 2L, 3L, new AssignedTo(AssignedToType.USER, 4L, "Tony Stark"),
            new DueDate(12, 2))));

    var workflowRequest = WorkflowStub.aWorkflowRequestWithActions("Workflow 1", "Workflow 1", LEAD, EVENT, CREATED, FOR_ALL, actions);

    var workflowMock = mock(Workflow.class);
    given(workflowMock.getId()).willReturn(1L);
    given(
        workflowFacade.create(argThat(request -> request.getActions().stream().anyMatch(action -> action.getType().equals(ActionType.CREATE_TASK)))))
        .willReturn(Mono.just(workflowMock));
    doNothing().when(workflowFacade).validate(workflowRequest);
    // when
    Mono<WorkflowSummary> workflowSummaryMono = workflowService.create(workflowRequest);
    // then
    verify(workflowFacade, times(1)).validate(workflowRequest);
    StepVerifier.create(workflowSummaryMono)
        .assertNext(workflowSummary -> assertThat(workflowSummary.getId()).isEqualTo(1L))
        .verifyComplete();
  }

  @Test
  public void givenDealWorkflowRequest_withCreateTaskAction_shouldCreateIt() {
    // given
    var actions = Set.of(new ActionResponse(CREATE_TASK,
        new CreateTaskAction("new Task", "new Task description", 1L, "contacted", 2L, 3L, new AssignedTo(AssignedToType.USER, 4L, "Tony Stark"),
            new DueDate(12, 2))));

    var workflowRequest = WorkflowStub.aWorkflowRequestWithActions("Workflow 1", "Workflow 1", DEAL, EVENT, CREATED, FOR_ALL, actions);

    var workflowMock = mock(Workflow.class);
    given(workflowMock.getId()).willReturn(1L);
    given(
        workflowFacade.create(argThat(request -> request.getActions().stream().anyMatch(action -> action.getType().equals(ActionType.CREATE_TASK)))))
        .willReturn(Mono.just(workflowMock));
    doNothing().when(workflowFacade).validate(workflowRequest);
    // when
    Mono<WorkflowSummary> workflowSummaryMono = workflowService.create(workflowRequest);
    // then
    verify(workflowFacade, times(1)).validate(workflowRequest);
    StepVerifier.create(workflowSummaryMono)
        .assertNext(workflowSummary -> assertThat(workflowSummary.getId()).isEqualTo(1L))
        .verifyComplete();
  }

  @Test
  public void givenContactWorkflowRequest_withCreateTaskAction_shouldCreateIt() {
    // given
    var actions = Set.of(new ActionResponse(CREATE_TASK,
        new CreateTaskAction("new Task", "new Task description", 1L, "contacted", 2L, 3L, new AssignedTo(AssignedToType.USER, 4L, "Tony Stark"),
            new DueDate(12, 2))));

    var workflowRequest = WorkflowStub.aWorkflowRequestWithActions("Workflow 1", "Workflow 1", CONTACT, EVENT, CREATED, FOR_ALL, actions);

    var workflowMock = mock(Workflow.class);
    given(workflowMock.getId()).willReturn(1L);
    given(
        workflowFacade.create(argThat(request -> request.getActions().stream().anyMatch(action -> action.getType().equals(ActionType.CREATE_TASK)))))
        .willReturn(Mono.just(workflowMock));
    doNothing().when(workflowFacade).validate(workflowRequest);
    // when
    Mono<WorkflowSummary> workflowSummaryMono = workflowService.create(workflowRequest);
    // then
    verify(workflowFacade, times(1)).validate(workflowRequest);
    StepVerifier.create(workflowSummaryMono)
        .assertNext(workflowSummary -> assertThat(workflowSummary.getId()).isEqualTo(1L))
        .verifyComplete();
  }

  @Test
  public void givenLeadWorkflowRequest_withSendEmailAction_shouldCreateIt() {
    // given
    Object from = "{\"type\":\"RECORD_OWNER\",\"entity\":\"user\",\"entityId\":1,\"name\":\"user1\",\"email\":\"user1@gmail.com\"}";
    List<Participant> participants = List
        .of(new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.LEAD.getEntityName(), 1L, "test user", "test@user.com"),
            new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.LEAD.getEntityName(), 2L, "test user", "test@user.com"));

    var actions = Set.of(new ActionResponse(SEND_EMAIL, new EmailAction(1L, from, participants, participants, participants)));

    var workflowRequest = WorkflowStub.aWorkflowRequestWithActions("Workflow 1", "Workflow 1", LEAD, EVENT, CREATED, FOR_ALL, actions);

    var workflowMock = mock(Workflow.class);
    given(workflowMock.getId()).willReturn(1L);
    given(workflowFacade.create(argThat(request -> request.getActions().stream().anyMatch(action -> action.getType().equals(SEND_EMAIL)))))
        .willReturn(Mono.just(workflowMock));
    doNothing().when(workflowFacade).validate(workflowRequest);
    // when
    Mono<WorkflowSummary> workflowSummaryMono = workflowService.create(workflowRequest);
    // then
    verify(workflowFacade, times(1)).validate(workflowRequest);
    StepVerifier.create(workflowSummaryMono)
        .assertNext(workflowSummary -> assertThat(workflowSummary.getId()).isEqualTo(1L))
        .verifyComplete();
  }

  @Test
  public void givenContactWorkflowRequest_withSendEmailAction_shouldCreateIt() {
    // given
    Object from = "{\"type\":\"RECORD_OWNER\",\"entity\":\"user\",\"entityId\":1,\"name\":\"user1\",\"email\":\"user1@gmail.com\"}";
    List<Participant> participants = List
        .of(new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.CONTACT.getEntityName(), 1L, "test user", "test@user.com"),
            new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.CONTACT.getEntityName(), 2L, "test user", "test@user.com"));

    var actions = Set.of(new ActionResponse(SEND_EMAIL, new EmailAction(1L, from, participants, participants, participants)));

    var workflowRequest = WorkflowStub.aWorkflowRequestWithActions("Workflow 1", "Workflow 1", CONTACT, EVENT, CREATED, FOR_ALL, actions);

    var workflowMock = mock(Workflow.class);
    given(workflowMock.getId()).willReturn(1L);
    given(workflowFacade.create(argThat(request -> request.getActions().stream().anyMatch(action -> action.getType().equals(SEND_EMAIL)))))
        .willReturn(Mono.just(workflowMock));
    doNothing().when(workflowFacade).validate(workflowRequest);
    // when
    Mono<WorkflowSummary> workflowSummaryMono = workflowService.create(workflowRequest);
    // then
    verify(workflowFacade, times(1)).validate(workflowRequest);
    StepVerifier.create(workflowSummaryMono)
        .assertNext(workflowSummary -> assertThat(workflowSummary.getId()).isEqualTo(1L))
        .verifyComplete();
  }

  @Test
  public void givenDealWorkflowRequest_withSendEmailAction_shouldCreateIt() {
    // given
    Object from = "{\"type\":\"RECORD_OWNER\",\"entity\":\"user\",\"entityId\":1,\"name\":\"user1\",\"email\":\"user1@gmail.com\"}";
    List<Participant> participants = List
        .of(new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.DEAL.getEntityName(), 1L, "test user", "test@user.com"),
            new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.DEAL.getEntityName(), 2L, "test user", "test@user.com"));

    var actions = Set.of(new ActionResponse(SEND_EMAIL, new EmailAction(1L, from, participants, participants, participants)));

    var workflowRequest = WorkflowStub.aWorkflowRequestWithActions("Workflow 1", "Workflow 1", DEAL, EVENT, CREATED, FOR_ALL, actions);

    var workflowMock = mock(Workflow.class);
    given(workflowMock.getId()).willReturn(1L);
    given(workflowFacade.create(argThat(request -> request.getActions().stream().anyMatch(action -> action.getType().equals(SEND_EMAIL)))))
        .willReturn(Mono.just(workflowMock));
    doNothing().when(workflowFacade).validate(workflowRequest);
    // when
    Mono<WorkflowSummary> workflowSummaryMono = workflowService.create(workflowRequest);
    // then
    verify(workflowFacade, times(1)).validate(workflowRequest);
    StepVerifier.create(workflowSummaryMono)
        .assertNext(workflowSummary -> assertThat(workflowSummary.getId()).isEqualTo(1L))
        .verifyComplete();
  }

  @Test
  public void givenWorkflowRequest_withCreateTaskActionAndAssignedToOwner_shouldCreateIt() {
    // given
    var actions = Set.of(new ActionResponse(CREATE_TASK,
        new CreateTaskAction("new Task", "new Task description", 1L, "contacted", 2L, 3L, new AssignedTo(AssignedToType.OWNER, null, "Tony Stark"),
            new DueDate(12, 2))));

    var workflowRequest = WorkflowStub.aWorkflowRequestWithActions("Workflow 1", "Workflow 1", LEAD, EVENT, CREATED, FOR_ALL, actions);

    var workflowMock = mock(Workflow.class);
    given(workflowMock.getId()).willReturn(1L);
    given(
        workflowFacade.create(argThat(request -> request.getActions().stream().anyMatch(action -> action.getType().equals(ActionType.CREATE_TASK)))))
        .willReturn(Mono.just(workflowMock));
    doNothing().when(workflowFacade).validate(workflowRequest);
    // when
    Mono<WorkflowSummary> workflowSummaryMono = workflowService.create(workflowRequest);
    // then
    verify(workflowFacade, times(1)).validate(workflowRequest);
    StepVerifier.create(workflowSummaryMono)
        .assertNext(workflowSummary -> assertThat(workflowSummary.getId()).isEqualTo(1L))
        .verifyComplete();
  }

  @Test
  public void givenWorkflowRequest_withCreateTaskActionAndAssignedToCreatedBy_shouldCreateIt() {
    // given
    var actions = Set.of(new ActionResponse(CREATE_TASK,
        new CreateTaskAction("new Task", "new Task description", 1L, "contacted", 2L, 3L,
            new AssignedTo(AssignedToType.CREATED_BY, null, "Tony Stark"),
            new DueDate(12, 2))));

    var workflowRequest = WorkflowStub.aWorkflowRequestWithActions("Workflow 1", "Workflow 1", LEAD, EVENT, CREATED, FOR_ALL, actions);

    var workflowMock = mock(Workflow.class);
    given(workflowMock.getId()).willReturn(1L);
    given(
        workflowFacade.create(argThat(request -> request.getActions().stream().anyMatch(action -> action.getType().equals(ActionType.CREATE_TASK)))))
        .willReturn(Mono.just(workflowMock));
    doNothing().when(workflowFacade).validate(workflowRequest);
    // when
    Mono<WorkflowSummary> workflowSummaryMono = workflowService.create(workflowRequest);
    // then
    verify(workflowFacade, times(1)).validate(workflowRequest);
    StepVerifier.create(workflowSummaryMono)
        .assertNext(workflowSummary -> assertThat(workflowSummary.getId()).isEqualTo(1L))
        .verifyComplete();
  }

  @Test
  public void givenWorkflowUpdateRequest_shouldUpdateIt() {
    // given
    var workflowRequestMock = mock(WorkflowRequest.class);
    User aUser = UserStub.aUser(11L, 99L, true, true, true, false, false).withName("user 1");
    WorkflowTrigger trigger =
        WorkflowTrigger.createNew(
            new com.kylas.sales.workflow.common.dto.WorkflowTrigger(
                EVENT, CREATED));

    var condition = new WorkflowCondition(FOR_ALL, null);
    Set<AbstractWorkflowAction> actions = new HashSet<>();
    EditPropertyAction editPropertyAction = new EditPropertyAction();
    UUID id = UUID.randomUUID();
    editPropertyAction.setId(id);
    editPropertyAction.setName("firstName");
    editPropertyAction.setValue("tony");
    actions.add(editPropertyAction);

    Workflow workflow =
        Workflow.createNew(
            "Workflow 1", "Workflow 1", LEAD, trigger, aUser, actions, condition, true);
    workflow.setId(1L);
    given(workflowFacade.update(1L, workflowRequestMock)).willReturn(Mono.just(workflow));
    doNothing().when(workflowFacade).validate(workflowRequestMock);
    // when
    Mono<WorkflowDetail> workflowDetailMono = workflowService.update(1L, workflowRequestMock);
    // then
    verify(workflowFacade, times(1)).validate(workflowRequestMock);
    StepVerifier.create(workflowDetailMono)
        .assertNext(workflowDetail -> {
          assertThat(workflowDetail.getId()).isEqualTo(workflow.getId());
          assertThat(workflowDetail.getActions()).hasSize(workflow.getWorkflowActions().size());
          assertThat(workflowDetail.getName()).isEqualTo("Workflow 1");
          assertThat(workflowDetail.getDescription()).isEqualTo("Workflow 1");
          assertThat(workflowDetail.getEntityType()).isEqualTo(workflow.getEntityType());
          assertThat(workflowDetail.getTrigger().getTriggerFrequency()).isEqualTo(workflow.getWorkflowTrigger().getTriggerFrequency());
          assertThat(workflowDetail.getCondition().getConditionType()).isEqualTo(workflow.getWorkflowCondition().getType());
          assertThat(workflowDetail.isActive()).isTrue();
        })
        .verifyComplete();
  }

  @Test
  public void givenLeadWorkflowUpdateRequest_withCreateTaskAction_shouldUpdateIt() {
    // given
    var workflowRequestMock = mock(WorkflowRequest.class);
    User aUser = UserStub.aUser(11L, 99L, true, true, true, false, false).withName("user 1");
    WorkflowTrigger trigger =
        WorkflowTrigger.createNew(
            new com.kylas.sales.workflow.common.dto.WorkflowTrigger(
                EVENT, CREATED));

    var condition = new WorkflowCondition(FOR_ALL, null);
    Set<AbstractWorkflowAction> actions = new HashSet<>();
    var createTaskActionMock = new com.kylas.sales.workflow.domain.workflow.action.task.CreateTaskAction("new Task", "new task description", 1L,
        "contacted", 2L, 3L, new AssignedTo(AssignedToType.USER, 4L, "Tony Stark"),
        new DueDate(12, 2));
    UUID id = UUID.randomUUID();
    createTaskActionMock.setId(id);
    actions.add(createTaskActionMock);

    Workflow workflow =
        Workflow.createNew(
            "Workflow 1", "Workflow 1", LEAD, trigger, aUser, actions, condition, true);
    workflow.setId(1L);
    given(valueResolver.getUserName(any(), any())).willReturn(Mono.just("James Bond"));
    given(workflowFacade.update(1L, workflowRequestMock)).willReturn(Mono.just(workflow));
    doNothing().when(workflowFacade).validate(workflowRequestMock);
    // when
    Mono<WorkflowDetail> workflowDetailMono = workflowService.update(1L, workflowRequestMock);
    // then
    verify(workflowFacade, times(1)).validate(workflowRequestMock);
    StepVerifier.create(workflowDetailMono)
        .assertNext(workflowDetail -> {
          assertThat(workflowDetail.getId()).isEqualTo(workflow.getId());
          assertThat(workflowDetail.getActions()).hasSize(workflow.getWorkflowActions().size());
          assertThat(workflowDetail.getName()).isEqualTo("Workflow 1");
          assertThat(workflowDetail.getDescription()).isEqualTo("Workflow 1");
          assertThat(workflowDetail.getEntityType()).isEqualTo(workflow.getEntityType());
          assertThat(workflowDetail.getTrigger().getTriggerFrequency()).isEqualTo(workflow.getWorkflowTrigger().getTriggerFrequency());
          assertThat(workflowDetail.getCondition().getConditionType()).isEqualTo(workflow.getWorkflowCondition().getType());
          com.kylas.sales.workflow.domain.workflow.action.task.CreateTaskAction createTaskAction = (com.kylas.sales.workflow.domain.workflow.action.task.CreateTaskAction) workflow
              .getWorkflowActions().iterator().next();

          assertThat(createTaskAction.getType()).isEqualTo(CREATE_TASK);
          assertThat(createTaskAction.getName()).isEqualTo("new Task");
          assertThat(createTaskAction.getDescription()).isEqualTo("new task description");
          assertThat(createTaskAction.getPriority()).isEqualTo(1L);
          assertThat(createTaskAction.getOutcome()).isEqualTo("contacted");
          assertThat(createTaskAction.getTaskType()).isEqualTo(2L);
          assertThat(createTaskAction.getStatus()).isEqualTo(3L);
          assertThat(createTaskAction.getAssignedTo().getId()).isEqualTo(4L);
          assertThat(createTaskAction.getDueDate().getDays()).isEqualTo(12);
          assertThat(createTaskAction.getDueDate().getHours()).isEqualTo(2);
          assertThat(workflowDetail.isActive()).isTrue();
        })
        .verifyComplete();
  }

  @Test
  public void givenWorkflowUpdateRequest_withCreateTaskActionAndAssignedToOwner_shouldUpdateIt() {
    // given
    var workflowRequestMock = mock(WorkflowRequest.class);
    User aUser = UserStub.aUser(11L, 99L, true, true, true, false, false).withName("user 1");
    WorkflowTrigger trigger =
        WorkflowTrigger.createNew(
            new com.kylas.sales.workflow.common.dto.WorkflowTrigger(
                EVENT, CREATED));

    var condition = new WorkflowCondition(FOR_ALL, null);
    Set<AbstractWorkflowAction> actions = new HashSet<>();
    var createTaskActionMock = new com.kylas.sales.workflow.domain.workflow.action.task.CreateTaskAction("new Task", "new task description", 1L,
        "contacted", 2L, 3L, new AssignedTo(AssignedToType.OWNER, null, "Tony Stark"),
        new DueDate(12, 2));
    UUID id = UUID.randomUUID();
    createTaskActionMock.setId(id);
    actions.add(createTaskActionMock);

    Workflow workflow =
        Workflow.createNew(
            "Workflow 1", "Workflow 1", LEAD, trigger, aUser, actions, condition, true);
    workflow.setId(1L);
    given(workflowFacade.update(1L, workflowRequestMock)).willReturn(Mono.just(workflow));
    doNothing().when(workflowFacade).validate(workflowRequestMock);
    // when
    Mono<WorkflowDetail> workflowDetailMono = workflowService.update(1L, workflowRequestMock);
    // then
    verify(workflowFacade, times(1)).validate(workflowRequestMock);
    StepVerifier.create(workflowDetailMono)
        .assertNext(workflowDetail -> {
          assertThat(workflowDetail.getId()).isEqualTo(workflow.getId());
          assertThat(workflowDetail.getActions()).hasSize(workflow.getWorkflowActions().size());
          assertThat(workflowDetail.getName()).isEqualTo("Workflow 1");
          assertThat(workflowDetail.getDescription()).isEqualTo("Workflow 1");
          assertThat(workflowDetail.getEntityType()).isEqualTo(workflow.getEntityType());
          assertThat(workflowDetail.getTrigger().getTriggerFrequency()).isEqualTo(workflow.getWorkflowTrigger().getTriggerFrequency());
          assertThat(workflowDetail.getCondition().getConditionType()).isEqualTo(workflow.getWorkflowCondition().getType());
          com.kylas.sales.workflow.domain.workflow.action.task.CreateTaskAction createTaskAction = (com.kylas.sales.workflow.domain.workflow.action.task.CreateTaskAction) workflow
              .getWorkflowActions().iterator().next();

          assertThat(createTaskAction.getType()).isEqualTo(CREATE_TASK);
          assertThat(createTaskAction.getName()).isEqualTo("new Task");
          assertThat(createTaskAction.getDescription()).isEqualTo("new task description");
          assertThat(createTaskAction.getPriority()).isEqualTo(1L);
          assertThat(createTaskAction.getOutcome()).isEqualTo("contacted");
          assertThat(createTaskAction.getTaskType()).isEqualTo(2L);
          assertThat(createTaskAction.getStatus()).isEqualTo(3L);
          assertThat(createTaskAction.getAssignedTo().getId()).isNull();
          assertThat(createTaskAction.getDueDate().getDays()).isEqualTo(12);
          assertThat(createTaskAction.getDueDate().getHours()).isEqualTo(2);
          assertThat(workflowDetail.isActive()).isTrue();
        })
        .verifyComplete();
  }

  @Test
  public void givenWorkflowUpdateRequest_withCreateTaskActionAndAssignedToCreatedBy_shouldUpdateIt() {
    // given
    var workflowRequestMock = mock(WorkflowRequest.class);
    User aUser = UserStub.aUser(11L, 99L, true, true, true, false, false).withName("user 1");
    WorkflowTrigger trigger =
        WorkflowTrigger.createNew(
            new com.kylas.sales.workflow.common.dto.WorkflowTrigger(
                EVENT, CREATED));

    var condition = new WorkflowCondition(FOR_ALL, null);
    Set<AbstractWorkflowAction> actions = new HashSet<>();
    var createTaskActionMock = new com.kylas.sales.workflow.domain.workflow.action.task.CreateTaskAction("new Task", "new task description", 1L,
        "contacted", 2L, 3L, new AssignedTo(AssignedToType.CREATED_BY, null, "Tony Stark"),
        new DueDate(12, 2));
    UUID id = UUID.randomUUID();
    createTaskActionMock.setId(id);
    actions.add(createTaskActionMock);

    Workflow workflow =
        Workflow.createNew(
            "Workflow 1", "Workflow 1", LEAD, trigger, aUser, actions, condition, true);
    workflow.setId(1L);
    given(workflowFacade.update(1L, workflowRequestMock)).willReturn(Mono.just(workflow));
    doNothing().when(workflowFacade).validate(workflowRequestMock);
    // when
    Mono<WorkflowDetail> workflowDetailMono = workflowService.update(1L, workflowRequestMock);
    // then
    verify(workflowFacade, times(1)).validate(workflowRequestMock);
    StepVerifier.create(workflowDetailMono)
        .assertNext(workflowDetail -> {
          assertThat(workflowDetail.getId()).isEqualTo(workflow.getId());
          assertThat(workflowDetail.getActions()).hasSize(workflow.getWorkflowActions().size());
          assertThat(workflowDetail.getName()).isEqualTo("Workflow 1");
          assertThat(workflowDetail.getDescription()).isEqualTo("Workflow 1");
          assertThat(workflowDetail.getEntityType()).isEqualTo(workflow.getEntityType());
          assertThat(workflowDetail.getTrigger().getTriggerFrequency()).isEqualTo(workflow.getWorkflowTrigger().getTriggerFrequency());
          assertThat(workflowDetail.getCondition().getConditionType()).isEqualTo(workflow.getWorkflowCondition().getType());
          com.kylas.sales.workflow.domain.workflow.action.task.CreateTaskAction createTaskAction = (com.kylas.sales.workflow.domain.workflow.action.task.CreateTaskAction) workflow
              .getWorkflowActions().iterator().next();

          assertThat(createTaskAction.getType()).isEqualTo(CREATE_TASK);
          assertThat(createTaskAction.getName()).isEqualTo("new Task");
          assertThat(createTaskAction.getDescription()).isEqualTo("new task description");
          assertThat(createTaskAction.getPriority()).isEqualTo(1L);
          assertThat(createTaskAction.getOutcome()).isEqualTo("contacted");
          assertThat(createTaskAction.getTaskType()).isEqualTo(2L);
          assertThat(createTaskAction.getStatus()).isEqualTo(3L);
          assertThat(createTaskAction.getAssignedTo().getId()).isNull();
          assertThat(createTaskAction.getDueDate().getDays()).isEqualTo(12);
          assertThat(createTaskAction.getDueDate().getHours()).isEqualTo(2);
          assertThat(workflowDetail.isActive()).isTrue();
        })
        .verifyComplete();
  }


  @Test
  public void givenDealWorkflowUpdateRequest_withCreateTaskAction_shouldUpdateIt() {
    // given
    var workflowRequestMock = mock(WorkflowRequest.class);
    User aUser = UserStub.aUser(11L, 99L, true, true, true, false, false).withName("user 1");
    WorkflowTrigger trigger =
        WorkflowTrigger.createNew(
            new com.kylas.sales.workflow.common.dto.WorkflowTrigger(
                EVENT, CREATED));

    var condition = new WorkflowCondition(FOR_ALL, null);
    Set<AbstractWorkflowAction> actions = new HashSet<>();
    var createTaskActionMock = new com.kylas.sales.workflow.domain.workflow.action.task.CreateTaskAction("new Task", "new task description", 1L,
        "contacted", 2L, 3L, new AssignedTo(AssignedToType.USER, 4L, "Tony Stark"),
        new DueDate(12, 2));
    UUID id = UUID.randomUUID();
    createTaskActionMock.setId(id);
    actions.add(createTaskActionMock);

    Workflow workflow =
        Workflow.createNew(
            "Workflow 1", "Workflow 1", DEAL, trigger, aUser, actions, condition, true);
    workflow.setId(1L);
    given(valueResolver.getUserName(any(), any())).willReturn(Mono.just("James Bond"));
    given(workflowFacade.update(1L, workflowRequestMock)).willReturn(Mono.just(workflow));
    doNothing().when(workflowFacade).validate(workflowRequestMock);
    // when
    Mono<WorkflowDetail> workflowDetailMono = workflowService.update(1L, workflowRequestMock);
    // then
    verify(workflowFacade, times(1)).validate(workflowRequestMock);
    StepVerifier.create(workflowDetailMono)
        .assertNext(workflowDetail -> {
          assertThat(workflowDetail.getId()).isEqualTo(workflow.getId());
          assertThat(workflowDetail.getActions()).hasSize(workflow.getWorkflowActions().size());
          assertThat(workflowDetail.getName()).isEqualTo("Workflow 1");
          assertThat(workflowDetail.getDescription()).isEqualTo("Workflow 1");
          assertThat(workflowDetail.getEntityType()).isEqualTo(workflow.getEntityType());
          assertThat(workflowDetail.getTrigger().getTriggerFrequency()).isEqualTo(workflow.getWorkflowTrigger().getTriggerFrequency());
          assertThat(workflowDetail.getCondition().getConditionType()).isEqualTo(workflow.getWorkflowCondition().getType());
          com.kylas.sales.workflow.domain.workflow.action.task.CreateTaskAction createTaskAction = (com.kylas.sales.workflow.domain.workflow.action.task.CreateTaskAction) workflow
              .getWorkflowActions().iterator().next();

          assertThat(createTaskAction.getType()).isEqualTo(CREATE_TASK);
          assertThat(createTaskAction.getName()).isEqualTo("new Task");
          assertThat(createTaskAction.getDescription()).isEqualTo("new task description");
          assertThat(createTaskAction.getPriority()).isEqualTo(1L);
          assertThat(createTaskAction.getOutcome()).isEqualTo("contacted");
          assertThat(createTaskAction.getTaskType()).isEqualTo(2L);
          assertThat(createTaskAction.getStatus()).isEqualTo(3L);
          assertThat(createTaskAction.getAssignedTo().getId()).isEqualTo(4L);
          assertThat(createTaskAction.getDueDate().getDays()).isEqualTo(12);
          assertThat(createTaskAction.getDueDate().getHours()).isEqualTo(2);
          assertThat(workflowDetail.isActive()).isTrue();
        })
        .verifyComplete();
  }

  @Test
  public void givenContactWorkflowUpdateRequest_withCreateTaskAction_shouldUpdateIt() {
    // given
    var workflowRequestMock = mock(WorkflowRequest.class);
    User aUser = UserStub.aUser(11L, 99L, true, true, true, false, false).withName("user 1");
    WorkflowTrigger trigger =
        WorkflowTrigger.createNew(
            new com.kylas.sales.workflow.common.dto.WorkflowTrigger(
                EVENT, CREATED));

    var condition = new WorkflowCondition(FOR_ALL, null);
    Set<AbstractWorkflowAction> actions = new HashSet<>();
    var createTaskActionMock = new com.kylas.sales.workflow.domain.workflow.action.task.CreateTaskAction("new Task", "new task description", 1L,
        "contacted", 2L, 3L, new AssignedTo(AssignedToType.USER, 4L, "Tony Stark"),
        new DueDate(12, 2));
    UUID id = UUID.randomUUID();
    createTaskActionMock.setId(id);
    actions.add(createTaskActionMock);

    Workflow workflow =
        Workflow.createNew(
            "Workflow 1", "Workflow 1", CONTACT, trigger, aUser, actions, condition, true);
    workflow.setId(1L);
    given(valueResolver.getUserName(any(), any())).willReturn(Mono.just("James Bond"));
    given(workflowFacade.update(1L, workflowRequestMock)).willReturn(Mono.just(workflow));
    doNothing().when(workflowFacade).validate(workflowRequestMock);
    // when
    Mono<WorkflowDetail> workflowDetailMono = workflowService.update(1L, workflowRequestMock);
    // then
    verify(workflowFacade, times(1)).validate(workflowRequestMock);
    StepVerifier.create(workflowDetailMono)
        .assertNext(workflowDetail -> {
          assertThat(workflowDetail.getId()).isEqualTo(workflow.getId());
          assertThat(workflowDetail.getActions()).hasSize(workflow.getWorkflowActions().size());
          assertThat(workflowDetail.getName()).isEqualTo("Workflow 1");
          assertThat(workflowDetail.getDescription()).isEqualTo("Workflow 1");
          assertThat(workflowDetail.getEntityType()).isEqualTo(workflow.getEntityType());
          assertThat(workflowDetail.getTrigger().getTriggerFrequency()).isEqualTo(workflow.getWorkflowTrigger().getTriggerFrequency());
          assertThat(workflowDetail.getCondition().getConditionType()).isEqualTo(workflow.getWorkflowCondition().getType());
          com.kylas.sales.workflow.domain.workflow.action.task.CreateTaskAction createTaskAction = (com.kylas.sales.workflow.domain.workflow.action.task.CreateTaskAction) workflow
              .getWorkflowActions().iterator().next();

          assertThat(createTaskAction.getType()).isEqualTo(CREATE_TASK);
          assertThat(createTaskAction.getName()).isEqualTo("new Task");
          assertThat(createTaskAction.getDescription()).isEqualTo("new task description");
          assertThat(createTaskAction.getPriority()).isEqualTo(1L);
          assertThat(createTaskAction.getOutcome()).isEqualTo("contacted");
          assertThat(createTaskAction.getTaskType()).isEqualTo(2L);
          assertThat(createTaskAction.getStatus()).isEqualTo(3L);
          assertThat(createTaskAction.getAssignedTo().getId()).isEqualTo(4L);
          assertThat(createTaskAction.getDueDate().getDays()).isEqualTo(12);
          assertThat(createTaskAction.getDueDate().getHours()).isEqualTo(2);
          assertThat(workflowDetail.isActive()).isTrue();
        })
        .verifyComplete();
  }

  @Test
  public void givenLeadWorkflowUpdateRequest_withSendEmailAction_shouldUpdateIt() {
    // given
    var workflowRequestMock = mock(WorkflowRequest.class);
    User aUser = UserStub.aUser(11L, 99L, true, true, true, false, false).withName("user 1");
    WorkflowTrigger trigger =
        WorkflowTrigger.createNew(
            new com.kylas.sales.workflow.common.dto.WorkflowTrigger(
                EVENT, CREATED));

    var condition = new WorkflowCondition(FOR_ALL, null);
    Set<AbstractWorkflowAction> actions = new HashSet<>();

    Object from = "{\"type\":\"RECORD_OWNER\",\"entity\":\"user\",\"entityId\":1,\"name\":\"user1\",\"email\":\"user1@gmail.com\"}";
    List<Participant> participants = List
        .of(new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.LEAD.getEntityName(), 1L, "test user", "test@user.com"),
            new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.LEAD.getEntityName(), 2L, "test user", "test@user.com"));

    var sendEmailActionMock = new com.kylas.sales.workflow.domain.workflow.action.email.EmailAction(1L, from, participants, participants,
        participants);
    UUID id = UUID.randomUUID();
    sendEmailActionMock.setId(id);
    actions.add(sendEmailActionMock);

    Workflow workflow =
        Workflow.createNew(
            "Workflow 1", "Workflow 1", LEAD, trigger, aUser, actions, condition, true);
    workflow.setId(1L);
    given(workflowFacade.update(1L, workflowRequestMock)).willReturn(Mono.just(workflow));
    doNothing().when(workflowFacade).validate(workflowRequestMock);
    // when
    Mono<WorkflowDetail> workflowDetailMono = workflowService.update(1L, workflowRequestMock);
    // then
    verify(workflowFacade, times(1)).validate(workflowRequestMock);
    StepVerifier.create(workflowDetailMono)
        .assertNext(workflowDetail -> {
          assertThat(workflowDetail.getId()).isEqualTo(workflow.getId());
          assertThat(workflowDetail.getActions()).hasSize(workflow.getWorkflowActions().size());
          assertThat(workflowDetail.getName()).isEqualTo("Workflow 1");
          assertThat(workflowDetail.getDescription()).isEqualTo("Workflow 1");
          assertThat(workflowDetail.getEntityType()).isEqualTo(workflow.getEntityType());
          assertThat(workflowDetail.getTrigger().getTriggerFrequency()).isEqualTo(workflow.getWorkflowTrigger().getTriggerFrequency());
          assertThat(workflowDetail.getCondition().getConditionType()).isEqualTo(workflow.getWorkflowCondition().getType());
          com.kylas.sales.workflow.domain.workflow.action.email.EmailAction emailAction = (com.kylas.sales.workflow.domain.workflow.action.email.EmailAction) workflow
              .getWorkflowActions().iterator().next();

          assertThat(emailAction.getType()).isEqualTo(SEND_EMAIL);
          assertThat(emailAction.getEmailTemplateId()).isEqualTo(1L);
          assertThat(emailAction.getFrom()).isEqualTo(from);
          assertThat(emailAction.getTo()).hasSize(2);
          assertThat(emailAction.getTo()).allMatch(
              participant -> participant.getType().equals(EmailActionType.RECORD_OWNER) && participant.getEntity()
                  .equals(EmailEntityType.LEAD.getEntityName()));
          assertThat(emailAction.getCc()).hasSize(2);
          assertThat(emailAction.getCc()).allMatch(
              participant -> participant.getType().equals(EmailActionType.RECORD_OWNER) && participant.getEntity()
                  .equals(EmailEntityType.LEAD.getEntityName()));
          assertThat(emailAction.getBcc()).hasSize(2);
          assertThat(emailAction.getBcc()).allMatch(
              participant -> participant.getType().equals(EmailActionType.RECORD_OWNER) && participant.getEntity()
                  .equals(EmailEntityType.LEAD.getEntityName()));

          assertThat(workflowDetail.isActive()).isTrue();
        })
        .verifyComplete();
  }

  @Test
  public void givenContactWorkflowUpdateRequest_withSendEmailAction_shouldUpdateIt() {
    // given
    var workflowRequestMock = mock(WorkflowRequest.class);
    User aUser = UserStub.aUser(11L, 99L, true, true, true, false, false).withName("user 1");
    WorkflowTrigger trigger =
        WorkflowTrigger.createNew(
            new com.kylas.sales.workflow.common.dto.WorkflowTrigger(
                EVENT, CREATED));

    var condition = new WorkflowCondition(FOR_ALL, null);
    Set<AbstractWorkflowAction> actions = new HashSet<>();

    Object from = "{\"type\":\"RECORD_OWNER\",\"entity\":\"user\",\"entityId\":1,\"name\":\"user1\",\"email\":\"user1@gmail.com\"}";
    List<Participant> participants = List
        .of(new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.CONTACT.getEntityName(), 1L, "test user", "test@user.com"),
            new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.CONTACT.getEntityName(), 2L, "test user", "test@user.com"));

    var sendEmailActionMock = new com.kylas.sales.workflow.domain.workflow.action.email.EmailAction(1L, from, participants, participants,
        participants);
    UUID id = UUID.randomUUID();
    sendEmailActionMock.setId(id);
    actions.add(sendEmailActionMock);

    Workflow workflow =
        Workflow.createNew(
            "Workflow 1", "Workflow 1", CONTACT, trigger, aUser, actions, condition, true);
    workflow.setId(1L);
    given(workflowFacade.update(1L, workflowRequestMock)).willReturn(Mono.just(workflow));
    doNothing().when(workflowFacade).validate(workflowRequestMock);
    // when
    Mono<WorkflowDetail> workflowDetailMono = workflowService.update(1L, workflowRequestMock);
    // then
    verify(workflowFacade, times(1)).validate(workflowRequestMock);
    StepVerifier.create(workflowDetailMono)
        .assertNext(workflowDetail -> {
          assertThat(workflowDetail.getId()).isEqualTo(workflow.getId());
          assertThat(workflowDetail.getActions()).hasSize(workflow.getWorkflowActions().size());
          assertThat(workflowDetail.getName()).isEqualTo("Workflow 1");
          assertThat(workflowDetail.getDescription()).isEqualTo("Workflow 1");
          assertThat(workflowDetail.getEntityType()).isEqualTo(workflow.getEntityType());
          assertThat(workflowDetail.getTrigger().getTriggerFrequency()).isEqualTo(workflow.getWorkflowTrigger().getTriggerFrequency());
          assertThat(workflowDetail.getCondition().getConditionType()).isEqualTo(workflow.getWorkflowCondition().getType());
          com.kylas.sales.workflow.domain.workflow.action.email.EmailAction emailAction = (com.kylas.sales.workflow.domain.workflow.action.email.EmailAction) workflow
              .getWorkflowActions().iterator().next();

          assertThat(emailAction.getType()).isEqualTo(SEND_EMAIL);
          assertThat(emailAction.getEmailTemplateId()).isEqualTo(1L);
          assertThat(emailAction.getFrom()).isEqualTo(from);
          assertThat(emailAction.getTo()).hasSize(2);
          assertThat(emailAction.getTo()).allMatch(
              participant -> participant.getType().equals(EmailActionType.RECORD_OWNER) && participant.getEntity()
                  .equals(EmailEntityType.CONTACT.getEntityName()));
          assertThat(emailAction.getCc()).hasSize(2);
          assertThat(emailAction.getCc()).allMatch(
              participant -> participant.getType().equals(EmailActionType.RECORD_OWNER) && participant.getEntity()
                  .equals(EmailEntityType.CONTACT.getEntityName()));
          assertThat(emailAction.getBcc()).hasSize(2);
          assertThat(emailAction.getBcc()).allMatch(
              participant -> participant.getType().equals(EmailActionType.RECORD_OWNER) && participant.getEntity()
                  .equals(EmailEntityType.CONTACT.getEntityName()));

          assertThat(workflowDetail.isActive()).isTrue();
        })
        .verifyComplete();
  }


  @Test
  public void givenDealWorkflowUpdateRequest_withSendEmailAction_shouldUpdateIt() {
    // given
    var workflowRequestMock = mock(WorkflowRequest.class);
    User aUser = UserStub.aUser(11L, 99L, true, true, true, false, false).withName("user 1");
    WorkflowTrigger trigger =
        WorkflowTrigger.createNew(
            new com.kylas.sales.workflow.common.dto.WorkflowTrigger(
                EVENT, CREATED));

    var condition = new WorkflowCondition(FOR_ALL, null);
    Set<AbstractWorkflowAction> actions = new HashSet<>();

    Object from = "{\"type\":\"RECORD_OWNER\",\"entity\":\"user\",\"entityId\":1,\"name\":\"user1\",\"email\":\"user1@gmail.com\"}";
    List<Participant> participants = List
        .of(new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.DEAL.getEntityName(), 1L, "test user", "test@user.com"),
            new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.DEAL.getEntityName(), 2L, "test user", "test@user.com"));

    var sendEmailActionMock = new com.kylas.sales.workflow.domain.workflow.action.email.EmailAction(1L, from, participants, participants,
        participants);
    UUID id = UUID.randomUUID();
    sendEmailActionMock.setId(id);
    actions.add(sendEmailActionMock);

    Workflow workflow =
        Workflow.createNew(
            "Workflow 1", "Workflow 1", DEAL, trigger, aUser, actions, condition, true);
    workflow.setId(1L);
    given(workflowFacade.update(1L, workflowRequestMock)).willReturn(Mono.just(workflow));
    doNothing().when(workflowFacade).validate(workflowRequestMock);
    // when
    Mono<WorkflowDetail> workflowDetailMono = workflowService.update(1L, workflowRequestMock);
    // then
    verify(workflowFacade, times(1)).validate(workflowRequestMock);
    StepVerifier.create(workflowDetailMono)
        .assertNext(workflowDetail -> {
          assertThat(workflowDetail.getId()).isEqualTo(workflow.getId());
          assertThat(workflowDetail.getActions()).hasSize(workflow.getWorkflowActions().size());
          assertThat(workflowDetail.getName()).isEqualTo("Workflow 1");
          assertThat(workflowDetail.getDescription()).isEqualTo("Workflow 1");
          assertThat(workflowDetail.getEntityType()).isEqualTo(workflow.getEntityType());
          assertThat(workflowDetail.getTrigger().getTriggerFrequency()).isEqualTo(workflow.getWorkflowTrigger().getTriggerFrequency());
          assertThat(workflowDetail.getCondition().getConditionType()).isEqualTo(workflow.getWorkflowCondition().getType());
          com.kylas.sales.workflow.domain.workflow.action.email.EmailAction emailAction = (com.kylas.sales.workflow.domain.workflow.action.email.EmailAction) workflow
              .getWorkflowActions().iterator().next();

          assertThat(emailAction.getType()).isEqualTo(SEND_EMAIL);
          assertThat(emailAction.getEmailTemplateId()).isEqualTo(1L);
          assertThat(emailAction.getFrom()).isEqualTo(from);
          assertThat(emailAction.getTo()).hasSize(2);
          assertThat(emailAction.getTo()).allMatch(
              participant -> participant.getType().equals(EmailActionType.RECORD_OWNER) && participant.getEntity()
                  .equals(EmailEntityType.DEAL.getEntityName()));
          assertThat(emailAction.getCc()).hasSize(2);
          assertThat(emailAction.getCc()).allMatch(
              participant -> participant.getType().equals(EmailActionType.RECORD_OWNER) && participant.getEntity()
                  .equals(EmailEntityType.DEAL.getEntityName()));
          assertThat(emailAction.getBcc()).hasSize(2);
          assertThat(emailAction.getBcc()).allMatch(
              participant -> participant.getType().equals(EmailActionType.RECORD_OWNER) && participant.getEntity()
                  .equals(EmailEntityType.DEAL.getEntityName()));

          assertThat(workflowDetail.isActive()).isTrue();
        })
        .verifyComplete();
  }


  @Test
  public void givenTenantAndEntityType_shouldReturnWorkflows() {
    // given
    long tenantId = 99;
    given(workflowFacade.findActiveBy(tenantId, LEAD, CREATED))
        .willReturn(Arrays.asList(mock(Workflow.class), mock(Workflow.class)));
    // when
    List<Workflow> workflows = workflowService.findActiveBy(tenantId, LEAD, CREATED);
    // then
    assertThat(workflows.size()).isEqualTo(2);
  }

  @Test
  public void givenWorkflowId_shouldGetWorkflowDetail() {
    // given
    long workflowId = 88L;
    User aUser = UserStub.aUser(11L, 99L, true, true, true, false, false).withName("user 1");
    WorkflowTrigger trigger =
        WorkflowTrigger.createNew(
            new com.kylas.sales.workflow.common.dto.WorkflowTrigger(
                EVENT, CREATED));

    var condition = new WorkflowCondition(FOR_ALL, null);
    Set<AbstractWorkflowAction> actions = new HashSet<>();
    EditPropertyAction editPropertyAction = new EditPropertyAction();
    UUID id = UUID.randomUUID();
    editPropertyAction.setId(id);
    editPropertyAction.setName("firstName");
    editPropertyAction.setValue("tony");
    actions.add(editPropertyAction);

    Workflow workflow =
        Workflow.createNew(
            "Workflow 1", "Workflow 1", LEAD, trigger, aUser, actions, condition, true);
    Workflow workflowSpy = Mockito.spy(workflow);
    given(workflowSpy.getId()).willReturn(workflowId);
    given(workflowFacade.get(workflowId)).willReturn(workflowSpy);
    given(authService.getAuthenticationToken()).willReturn("some-token");
    // when
    var workflowDetail = workflowService.get(workflowId).block();
    // then
    assertThat(workflowDetail.getId()).isEqualTo(workflowId);
    assertThat(workflowDetail.getName()).isEqualTo("Workflow 1");
    assertThat(workflowDetail.getDescription()).isEqualTo("Workflow 1");
    assertThat(workflowDetail.getEntityType()).isEqualTo(LEAD);

    assertThat(workflowDetail.getTrigger().getName()).isEqualTo(EVENT);
    assertThat(workflowDetail.getTrigger().getTriggerFrequency())
        .isEqualTo(CREATED);

    assertThat(workflowDetail.getActions().size()).isEqualTo(1);
    ActionResponse response = workflowDetail.getActions().iterator().next();
    assertThat(response.getType()).isEqualTo(EDIT_PROPERTY);
    var payload =
        (ActionDetail.EditPropertyAction) response.getPayload();
    assertThat(payload.getName()).isEqualTo("firstName");
    assertThat(payload.getValue()).isEqualTo("tony");
  }

  @Test
  public void givenWorkflowId_shouldActivateWorkflow() {
    // given
    long workflowId = 1000L;
    Workflow workflow = aWorkflowActivationStub(true);
    given(workflowFacade.activate(workflowId)).willReturn(workflow);
    // when
    var activatedWorkflow = workflowService.activate(workflowId).block();
    // then
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
        WorkflowTrigger.createNew(
            new com.kylas.sales.workflow.common.dto.WorkflowTrigger(
                EVENT, CREATED)));
    workflow.setWorkflowCondition(new WorkflowCondition(FOR_ALL, null));
    workflow.setWorkflowActions(Collections.emptySet());
    workflow.setCreatedBy(new User(4000L, 1000L, Collections.emptySet()));
    workflow.setUpdatedBy(new User(4000L, 1000L, Collections.emptySet()));
    var workflowExecutedEvent = new WorkflowExecutedEvent();
    workflowExecutedEvent.setLastTriggeredAt(new Date());
    workflowExecutedEvent.setTriggerCount(30L);
    workflow.setWorkflowExecutedEvent(workflowExecutedEvent);
    return workflow;
  }

  @Test
  public void givenWorkflowId_shouldDeactivateWorkflow() {
    // given
    long workflowId = 1000L;
    Workflow workflow = aWorkflowActivationStub(false);
    given(workflowFacade.deactivate(workflowId)).willReturn(workflow);
    // when
    var activatedWorkflow = workflowService.deactivate(workflowId).block();
    // then
    assertThat(activatedWorkflow.isActive()).isFalse();
    assertThat(activatedWorkflow.getId()).isEqualTo(1000L);
    assertThat(activatedWorkflow.getName()).isEqualTo("Workflow on bills");
  }

  @Test
  public void shouldGetWorkflowDetailList() {
    // given
    User aUser = UserStub.aUser(11L, 99L, true, true, true, false, false).withName("user 1");
    WorkflowTrigger trigger =
        WorkflowTrigger.createNew(
            new com.kylas.sales.workflow.common.dto.WorkflowTrigger(
                EVENT, CREATED));
    WorkflowCondition condition = new WorkflowCondition(FOR_ALL, null);
    Set<AbstractWorkflowAction> actions = new HashSet<>();
    EditPropertyAction editPropertyAction = new EditPropertyAction();
    UUID id = UUID.randomUUID();
    editPropertyAction.setId(id);
    editPropertyAction.setName("firstName");
    editPropertyAction.setValue("tony");
    actions.add(editPropertyAction);

    Workflow workflow =
        Workflow.createNew(
            "Workflow 1", "Workflow 1", LEAD, trigger, aUser, actions, condition, true);
    workflow.setAllowedActionsForUser(aUser);
    Workflow workflowSpy = Mockito.spy(workflow);
    given(workflowSpy.getId()).willReturn(100L);
    List<Workflow> workflows = new ArrayList<>();
    workflows.add(workflowSpy);

    PageImpl<Workflow> workflowPageable = new PageImpl<>(workflows, PageRequest.of(0, 10), 12);

    given(workflowFacade.list(argThat(new PageableMatcher(0, 10, Sort.unsorted()))))
        .willReturn(workflowPageable);
    PageRequest pageable = PageRequest.of(0, 10);
    // when
    Mono<Page<WorkflowDetail>> workflowPages = workflowService.list(pageable);
    // then
    StepVerifier.create(workflowPages)
        .assertNext(
            workflowDetails -> {
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

              assertThat(workflowDetail.getTrigger().getName()).isEqualTo(EVENT);
              assertThat(workflowDetail.getTrigger().getTriggerFrequency())
                  .isEqualTo(CREATED);

              assertThat(workflowDetail.getActions().size()).isEqualTo(1);
              ActionResponse response = workflowDetail.getActions().iterator().next();
              assertThat(response.getType()).isEqualTo(EDIT_PROPERTY);
              var payload =
              (ActionDetail.EditPropertyAction) response.getPayload();
          assertThat(payload.getName()).isEqualTo("firstName");
              assertThat(payload.getValue()).isEqualTo("tony");

              assertThat(workflowDetail.getAllowedActions().canRead()).isTrue();
            })
        .verifyComplete();
  }

  @Test
  public void givenSearchRequest_tryToSortOnLastTriggeredAt_shouldGet() {
    // given
    User aUser = UserStub.aUser(11L, 99L, true, true, true, false, false).withName("user 1");
    WorkflowTrigger trigger =
        WorkflowTrigger.createNew(
            new com.kylas.sales.workflow.common.dto.WorkflowTrigger(
                EVENT, CREATED));
    WorkflowCondition condition = new WorkflowCondition(FOR_ALL, null);
    Set<AbstractWorkflowAction> actions = new HashSet<>();
    EditPropertyAction editPropertyAction = new EditPropertyAction();
    UUID id = UUID.randomUUID();
    editPropertyAction.setId(id);
    editPropertyAction.setName("firstName");
    editPropertyAction.setValue("tony");
    actions.add(editPropertyAction);

    Workflow workflow =
        Workflow.createNew(
            "Workflow 1", "Workflow 1", LEAD, trigger, aUser, actions, condition, true);
    workflow.setAllowedActionsForUser(aUser);
    Workflow workflowSpy = Mockito.spy(workflow);
    given(workflowSpy.getId()).willReturn(100L);
    List<Workflow> workflows = new ArrayList<>();
    workflows.add(workflowSpy);

    Sort sortByLastTriggeredAt = Sort.by(Order.desc("lastTriggeredAt"));
    PageImpl<Workflow> workflowPageable =
        new PageImpl<>(workflows, PageRequest.of(0, 10, sortByLastTriggeredAt), 12);
    given(workflowFacade.search(argThat(new PageableMatcher(0, 10, sortByLastTriggeredAt)), argThat(workflowFilters -> workflowFilters.isEmpty())))
        .willReturn(workflowPageable);
    PageRequest pageable = PageRequest.of(0, 10, sortByLastTriggeredAt);
    // when
    Page<WorkflowEntry> workflowEntries = workflowService.search(pageable, Optional.empty());

    assertThat(workflowEntries.getTotalElements()).isEqualTo(12);
    assertThat(workflowEntries.getNumber()).isEqualTo(0);
    assertThat(workflowEntries.getTotalPages()).isEqualTo(2);
    assertThat(workflowEntries.isFirst()).isTrue();
    assertThat(workflowEntries.isLast()).isFalse();

    assertThat(workflowEntries.getContent().size()).isEqualTo(1);

    WorkflowEntry workflowDetail = workflowEntries.getContent().get(0);
    assertThat(workflowDetail.getId()).isEqualTo(100L);
    assertThat(workflowDetail.getName()).isEqualTo("Workflow 1");
    assertThat(workflowDetail.getEntityType()).isEqualTo(LEAD);
    assertThat(workflowDetail.getAllowedActions().canRead()).isTrue();
  }

  @Test
  public void givenWorkflow_shouldUpdateExecutedEvent() {
    // given
    User aUser = UserStub.aUser(11L, 99L, true, true, true, false, false).withName("user 1");
    WorkflowTrigger trigger =
        WorkflowTrigger.createNew(
            new com.kylas.sales.workflow.common.dto.WorkflowTrigger(
                EVENT, CREATED));
    var condition = new WorkflowCondition(FOR_ALL, null);
    Set<AbstractWorkflowAction> actions = new HashSet<>();
    EditPropertyAction editPropertyAction = new EditPropertyAction();
    UUID id = UUID.randomUUID();
    editPropertyAction.setId(id);
    editPropertyAction.setName("firstName");
    editPropertyAction.setValue("tony");
    actions.add(editPropertyAction);

    Workflow workflow =
        Workflow.createNew(
            "Workflow 1", "Workflow 1", LEAD, trigger, aUser, actions, condition, true);
    BDDMockito.doNothing().when(workflowFacade).updateExecutedEvent(workflow);
    // when
    workflowService.updateExecutedEventDetails(workflow);
    // then
    Mockito.verify(workflowFacade, times(1)).updateExecutedEvent(any(Workflow.class));
  }

  @Test
  public void givenSearchRequest_tryToFilterOnStatus_shouldGet() {
    // given
    User aUser = UserStub.aUser(11L, 99L, true, true, true, false, false).withName("user 1");
    WorkflowTrigger trigger =
        WorkflowTrigger.createNew(
            new com.kylas.sales.workflow.common.dto.WorkflowTrigger(
                EVENT, CREATED));
    var condition = new WorkflowCondition(FOR_ALL, null);
    Set<AbstractWorkflowAction> actions = new HashSet<>();
    EditPropertyAction editPropertyAction = new EditPropertyAction();
    UUID id = UUID.randomUUID();
    editPropertyAction.setId(id);
    editPropertyAction.setName("firstName");
    editPropertyAction.setValue("tony");
    actions.add(editPropertyAction);

    Workflow workflow =
        Workflow.createNew(
            "Workflow 1", "Workflow 1", LEAD, trigger, aUser, actions, condition, true);
    workflow.setAllowedActionsForUser(aUser);
    Workflow workflowSpy = Mockito.spy(workflow);
    given(workflowSpy.getId()).willReturn(100L);
    List<Workflow> workflows = new ArrayList<>();
    workflows.add(workflowSpy);

    Sort sortByLastTriggeredAt = Sort.by(Order.desc("lastTriggeredAt"));
    PageImpl<Workflow> workflowPageable =
        new PageImpl<>(workflows, PageRequest.of(0, 10, sortByLastTriggeredAt), 12);

    List<Filter> filters = new ArrayList<>();
    filters.add(new Filter("equals", "status", "string", "Active"));
    FilterRequest filterRequest = new FilterRequest(filters);

    given(workflowFacade
        .search(argThat(new PageableMatcher(0, 10, sortByLastTriggeredAt)), argThat(workflowFilters -> workflowFilters.get().size() == 1)))
        .willReturn(workflowPageable);
    PageRequest pageable = PageRequest.of(0, 10, sortByLastTriggeredAt);
    // when
    Page<WorkflowEntry> workflowEntries = workflowService.search(pageable, Optional.of(filterRequest));

    // then
    assertThat(workflowEntries.getTotalElements()).isEqualTo(12);
    assertThat(workflowEntries.getNumber()).isEqualTo(0);
    assertThat(workflowEntries.getTotalPages()).isEqualTo(2);
    assertThat(workflowEntries.isFirst()).isTrue();
    assertThat(workflowEntries.isLast()).isFalse();

    assertThat(workflowEntries.getContent().size()).isEqualTo(1);

    WorkflowEntry workflowDetail = workflowEntries.getContent().get(0);
    assertThat(workflowDetail.getId()).isEqualTo(100L);
    assertThat(workflowDetail.getName()).isEqualTo("Workflow 1");
    assertThat(workflowDetail.getEntityType()).isEqualTo(LEAD);
  }
}
