package com.kylas.sales.workflow.domain.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.kylas.sales.workflow.domain.exception.InsufficientPrivilegeException;
import com.kylas.sales.workflow.domain.exception.InvalidWorkflowPropertyException;
import com.kylas.sales.workflow.domain.user.User;
import java.util.HashSet;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class WorkflowTest {

  @Test
  public void givenUserWithoutHavingCreatePermission_tryToCreate_shouldThrow() {
    //given
    WorkflowTrigger triggerMock = Mockito.mock(WorkflowTrigger.class);
    User userMock = Mockito.mock(User.class);
    given(userMock.canCreateWorkflow()).willReturn(false);
    HashSet workflowActions = Mockito.mock(HashSet.class);
    WorkflowCondition conditionMock = Mockito.mock(WorkflowCondition.class);
    //when & then
    Assertions.assertThatThrownBy(
        () -> Workflow.createNew("Workflow1", "Workflow1", EntityType.LEAD, triggerMock, userMock, workflowActions, conditionMock, true))
        .isInstanceOf(InsufficientPrivilegeException.class);
  }

  @Test
  public void givenWorkflowRequest_shouldCreateIt() {
    //given
    WorkflowTrigger triggerMock = Mockito.mock(WorkflowTrigger.class);
    User userMock = Mockito.mock(User.class);
    given(userMock.canCreateWorkflow()).willReturn(true);
    HashSet workflowActions = Mockito.mock(HashSet.class);
    WorkflowCondition conditionMock = Mockito.mock(WorkflowCondition.class);
    //when
    Workflow aNew = Workflow.createNew("Workflow1", "Workflow1", EntityType.LEAD, triggerMock, userMock, workflowActions, conditionMock, true);
    //then
    assertThat(aNew).isNotNull();
  }

  @Test
  public void tryToCreateWorkflow_withInvalidEntity_shouldThrow() {
    //given
    WorkflowTrigger triggerMock = Mockito.mock(WorkflowTrigger.class);
    User userMock = Mockito.mock(User.class);
    given(userMock.canCreateWorkflow()).willReturn(true);
    HashSet workflowActions = Mockito.mock(HashSet.class);
    WorkflowCondition conditionMock = Mockito.mock(WorkflowCondition.class);
    //when & then
    Assertions.assertThatThrownBy(() -> Workflow.createNew("Workflow1", "Workflow1", null, triggerMock, userMock, workflowActions, conditionMock,
        true))
        .isInstanceOf(InvalidWorkflowPropertyException.class);
  }

  @Test
  public void tryToCreateWorkflow_withInvalidTrigger_shouldThrow() {
    //given
    User userMock = Mockito.mock(User.class);
    given(userMock.canCreateWorkflow()).willReturn(true);
    HashSet workflowActions = Mockito.mock(HashSet.class);
    WorkflowCondition conditionMock = Mockito.mock(WorkflowCondition.class);
    //when & then
    Assertions.assertThatThrownBy(() -> Workflow.createNew("Workflow1", "Workflow1", EntityType.LEAD, null, userMock, workflowActions, conditionMock,
        true))
        .isInstanceOf(InvalidWorkflowPropertyException.class);
  }

  @Test
  public void tryToCreateWorkflow_withInvalidAction_shouldThrow() {
    //given
    WorkflowTrigger triggerMock = Mockito.mock(WorkflowTrigger.class);
    User userMock = Mockito.mock(User.class);
    given(userMock.canCreateWorkflow()).willReturn(true);
    WorkflowCondition conditionMock = Mockito.mock(WorkflowCondition.class);
    //when & then
    Assertions.assertThatThrownBy(() -> Workflow.createNew("Workflow1", "Workflow1", EntityType.LEAD, triggerMock, userMock, null, conditionMock,
        true))
        .isInstanceOf(InvalidWorkflowPropertyException.class);
  }

  @Test
  public void tryToCreateWorkflow_withInvalidCondition_shouldThrow() {
    //given
    WorkflowTrigger triggerMock = Mockito.mock(WorkflowTrigger.class);
    User userMock = Mockito.mock(User.class);
    given(userMock.canCreateWorkflow()).willReturn(true);
    HashSet workflowActions = Mockito.mock(HashSet.class);
    //when & then
    Assertions.assertThatThrownBy(() -> Workflow.createNew("Workflow1", "Workflow1", EntityType.LEAD, triggerMock, userMock, workflowActions, null,
        true))
        .isInstanceOf(InvalidWorkflowPropertyException.class);
  }

  @Test
  public void givenUserWithReadPermission_shouldGetReadAction() {
    //given
    WorkflowTrigger triggerMock = Mockito.mock(WorkflowTrigger.class);
    User userMock = Mockito.mock(User.class);
    given(userMock.canCreateWorkflow()).willReturn(true);
    given(userMock.canQueryHisWorkflow()).willReturn(true);
    HashSet workflowActions = Mockito.mock(HashSet.class);
    WorkflowCondition conditionMock = Mockito.mock(WorkflowCondition.class);
    //when
    Workflow workflow = Workflow.createNew("Workflow1", "Workflow1", EntityType.LEAD, triggerMock, userMock, workflowActions, conditionMock, true)
        .setAllowedActionsForUser(userMock);
    //then
    assertThat(workflow.getAllowedActions().canRead()).isTrue();
  }

  @Test
  public void givenUserWithReadAllPermission_shouldGetReadAndReadAllAction() {
    //given
    WorkflowTrigger triggerMock = Mockito.mock(WorkflowTrigger.class);
    User userMock = Mockito.mock(User.class);
    given(userMock.canCreateWorkflow()).willReturn(true);
    given(userMock.canQueryAllWorkflow()).willReturn(true);
    HashSet workflowActions = Mockito.mock(HashSet.class);
    WorkflowCondition conditionMock = Mockito.mock(WorkflowCondition.class);
    //when
    Workflow workflow = Workflow.createNew("Workflow1", "Workflow1", EntityType.LEAD, triggerMock, userMock, workflowActions, conditionMock, true)
        .setAllowedActionsForUser(userMock);
    //then
    assertThat(workflow.getAllowedActions().canRead()).isTrue();
  }

  @Test
  public void givenUserWithUpdateAllPermission_shouldGetUpdateAndUpdateAllAction() {
    //given
    var triggerMock = Mockito.mock(WorkflowTrigger.class);
    var userMock = Mockito.mock(User.class);
    given(userMock.canCreateWorkflow()).willReturn(true);
    given(userMock.canUpdateAllWorkflow()).willReturn(true);
    var workflowActions = Mockito.mock(HashSet.class);
    var conditionMock = Mockito.mock(WorkflowCondition.class);
    //when
    Workflow workflow = Workflow.createNew("Workflow1", "Workflow1", EntityType.LEAD, triggerMock, userMock, workflowActions, conditionMock, true)
        .setAllowedActionsForUser(userMock);
    //then
    assertThat(workflow.getAllowedActions().canUpdate()).isTrue();
  }

  @Test
  public void givenUserWithUpdatePermission_shouldGetUpdateAction() {
    //given
    var triggerMock = Mockito.mock(WorkflowTrigger.class);
    var userMock = Mockito.mock(User.class);
    given(userMock.canCreateWorkflow()).willReturn(true);
    given(userMock.canUpdateHisWorkflow()).willReturn(true);
    var workflowActions = Mockito.mock(HashSet.class);
    var conditionMock = Mockito.mock(WorkflowCondition.class);
    //when
    Workflow workflow = Workflow.createNew("Workflow1", "Workflow1", EntityType.LEAD, triggerMock, userMock, workflowActions, conditionMock, true)
        .setAllowedActionsForUser(userMock);
    //then
    assertThat(workflow.getAllowedActions().canUpdate()).isTrue();
  }

  @Test
  public void givenNonCreatorUser_shouldGetAllPermissionToFalse() {
    //given
    WorkflowTrigger triggerMock = Mockito.mock(WorkflowTrigger.class);
    User creatorMock = Mockito.mock(User.class);
    given(creatorMock.canCreateWorkflow()).willReturn(true);

    User readerMock = Mockito.mock(User.class);

    HashSet workflowActions = Mockito.mock(HashSet.class);
    WorkflowCondition conditionMock = Mockito.mock(WorkflowCondition.class);
    //when
    Workflow workflow = Workflow.createNew("Workflow1", "Workflow1", EntityType.LEAD, triggerMock, creatorMock, workflowActions, conditionMock, true)
        .setAllowedActionsForUser(readerMock);
    //then
    assertThat(workflow.getAllowedActions().canRead()).isFalse();
    assertThat(workflow.getAllowedActions().canReadAll()).isFalse();
    assertThat(workflow.getAllowedActions().canWrite()).isFalse();
  }
}