package com.kylas.sales.workflow.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import com.kylas.sales.workflow.domain.exception.InsufficientPrivilegeException;
import com.kylas.sales.workflow.domain.exception.WorkflowNotFoundException;
import com.kylas.sales.workflow.domain.service.UserService;
import com.kylas.sales.workflow.domain.user.User;
import com.kylas.sales.workflow.domain.user.UserFacade;
import com.kylas.sales.workflow.domain.workflow.ConditionType;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.TriggerFrequency;
import com.kylas.sales.workflow.domain.workflow.TriggerType;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType;
import com.kylas.sales.workflow.security.AuthService;
import com.kylas.sales.workflow.stubs.UserStub;
import com.kylas.sales.workflow.stubs.WorkflowStub;
import java.util.List;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(initializers = {TestDatabaseInitializer.class})
class WorkflowFacadeTest {

  @MockBean
  AuthService authService;
  @MockBean
  UserService userService;
  @Autowired
  UserFacade userFacade;
  @Autowired
  WorkflowFacade workflowFacade;

  @Transactional
  @Test
  @Sql("/test-scripts/insert-users.sql")
  public void givenWorkflowRequest_shouldCreateIt() {
    //given
    User aUser = UserStub.aUser(11L, 99L, true, true, true, true, true)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    given(userService.getUserDetails(11L, authService.getAuthenticationToken()))
        .willReturn(
            Mono.just(
                aUser));
    var workflowRequest = WorkflowStub
        .anEditPropertyWorkflowRequest("Edit Lead Property", "Edit Lead Property", EntityType.LEAD, TriggerType.EVENT, TriggerFrequency.CREATED,
            ConditionType.FOR_ALL, ActionType.EDIT_PROPERTY, "lastName", "Stark");
    //when
    var workflowMono = workflowFacade.create(workflowRequest);
    //then
    StepVerifier.create(workflowMono)
        .expectNextMatches(workflow -> {
          assertThat(workflow.getId()).isEqualTo(1L);

          assertThat(workflow.getWorkflowTrigger().getTriggerFrequency()).isEqualTo(TriggerFrequency.CREATED);
          assertThat(workflow.getWorkflowTrigger().getTriggerType()).isEqualTo(TriggerType.EVENT);

          assertThat(workflow.getWorkflowActions().size()).isEqualTo(1);
          EditPropertyAction workflowAction = (EditPropertyAction) workflow.getWorkflowActions().iterator().next();
          assertThat(workflowAction.getName()).isEqualTo("lastName");
          assertThat(workflowAction.getValue()).isEqualTo("Stark");

          assertThat(workflow.getWorkflowCondition().getType()).isEqualTo(ConditionType.FOR_ALL);
          return true;
        })
        .verifyComplete();
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-users.sql")
  public void givenUserWithoutCreatePermission_tryToCreate_shouldThrow() {
    //given
    User aUser = UserStub.aUser(11L, 99L, false, true, true, true, true)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    given(userService.getUserDetails(11L, authService.getAuthenticationToken()))
        .willReturn(
            Mono.just(
                aUser));
    var workflowRequest = WorkflowStub
        .anEditPropertyWorkflowRequest("Edit Lead Property", "Edit Lead Property", EntityType.LEAD, TriggerType.EVENT, TriggerFrequency.CREATED,
            ConditionType.FOR_ALL, ActionType.EDIT_PROPERTY, "lastName", "Stark");
    //when
    var workflowMono = workflowFacade.create(workflowRequest);
    //then
    StepVerifier.create(workflowMono)
        .verifyErrorMatches(throwable -> throwable instanceof InsufficientPrivilegeException);

  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-workflow.sql")
  public void givenTenantIdAndEntityType_shouldReturn() {
    //given
    long tenantId = 99L;
    User aUser = UserStub.aUser(12, tenantId, false, true, true, true, true)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    //when
    var workflows = workflowFacade.findAllBy(tenantId, EntityType.LEAD);
    //then
    assertThat(workflows.size()).isEqualTo(2);
    List<Long> workflowIds = workflows.stream().map(workflow -> workflow.getId()).collect(Collectors.toList());

    assertThat(workflowIds).containsExactlyInAnyOrder(301L, 302L);
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-create-lead-workflow.sql")
  public void givenWorkflowId_shouldGetIt() {
    //given
    long tenantId = 99L;
    long userId = 12L;
    User aUser = UserStub.aUser(userId, tenantId, false, true, true, false, false)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);
    long workflowId = 301;
    //when
    var workflow = workflowFacade.get(workflowId);
    //then
    assertThat(workflow.getId()).isEqualTo(workflowId);
    assertThat(workflow.getAllowedActions().canRead()).isEqualTo(true);
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-create-lead-workflow.sql")
  public void givenNonExistingWorkflowId_tryToGet_shouldThrow() {
    //given
    long tenantId = 99L;
    long userId = 12L;
    User aUser = UserStub.aUser(userId, tenantId, false, true, true, false, false)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);
    long workflowId = 999;
    //when
    Assertions.assertThatThrownBy(() -> workflowFacade.get(workflowId))
        .isInstanceOf(WorkflowNotFoundException.class)
        .hasMessage("01701005");
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-create-lead-workflow.sql")
  public void givenUserDoesNotHaveReadAndReadAllPermission_tryToGetWorkflow_shouldThrow() {
    //given
    long tenantId = 99L;
    long userId = 12L;
    User aUser = UserStub.aUser(userId, tenantId, false, false, false, false, false)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);
    long workflowId = 301;
    //when
    //then
    Assertions.assertThatThrownBy(() -> workflowFacade.get(workflowId))
        .isInstanceOf(InsufficientPrivilegeException.class)
        .hasMessage("01702001");
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-lead-workflow-for-multiple-users.sql")
  public void givenUserHavingReadAllPermission_tryToGetOthersWorkflow_shouldGet() {
    //given
    long tenantId = 99L;
    long userId = 12L;
    User aUser = UserStub.aUser(userId, tenantId, false, true, true, false, false)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);
    long workflowIdForUser13 = 302;
    //when
    Workflow workflow = workflowFacade.get(workflowIdForUser13);
    //then
    assertThat(workflow.getId()).isEqualTo(workflowIdForUser13);
    assertThat(workflow.getAllowedActions().canRead()).isEqualTo(true);
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-lead-workflow-for-multiple-users.sql")
  public void shouldGetAllWorkflowAsPageableResponse() {
    //given
    long tenantId = 99L;
    long userId = 12L;
    User aUser = UserStub.aUser(userId, tenantId, false, true, true, false, false)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    PageRequest pageable = PageRequest.of(0, 10);
    //when
    Page<Workflow> pageResponse = workflowFacade.list(pageable);
    //then
    assertThat(pageResponse.getTotalElements()).isEqualTo(2);
  }
}