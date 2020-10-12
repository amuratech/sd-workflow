package com.kylas.sales.workflow.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import com.kylas.sales.workflow.domain.exception.InsufficientPrivilegeException;
import com.kylas.sales.workflow.domain.service.UserService;
import com.kylas.sales.workflow.domain.user.User;
import com.kylas.sales.workflow.domain.user.UserFacade;
import com.kylas.sales.workflow.domain.workflow.ConditionType;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.TriggerFrequency;
import com.kylas.sales.workflow.domain.workflow.TriggerType;
import com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType;
import com.kylas.sales.workflow.security.AuthService;
import com.kylas.sales.workflow.stubs.UserStub;
import com.kylas.sales.workflow.stubs.WorkflowStub;
import javax.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
}