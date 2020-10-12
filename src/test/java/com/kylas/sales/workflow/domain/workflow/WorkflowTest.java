package com.kylas.sales.workflow.domain.workflow;

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
        () -> Workflow.createNew("Workflow1", "Workflow1", EntityType.LEAD, triggerMock, userMock, workflowActions, conditionMock))
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
    Workflow aNew = Workflow.createNew("Workflow1", "Workflow1", EntityType.LEAD, triggerMock, userMock, workflowActions, conditionMock);
    //then
    Assertions.assertThat(aNew).isNotNull();
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
    Assertions.assertThatThrownBy(() -> Workflow.createNew("Workflow1", "Workflow1", null, triggerMock, userMock, workflowActions, conditionMock))
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
    Assertions.assertThatThrownBy(() -> Workflow.createNew("Workflow1", "Workflow1", EntityType.LEAD, null, userMock, workflowActions, conditionMock))
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
    Assertions.assertThatThrownBy(() -> Workflow.createNew("Workflow1", "Workflow1", EntityType.LEAD, triggerMock, userMock, null, conditionMock))
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
    Assertions.assertThatThrownBy(() -> Workflow.createNew("Workflow1", "Workflow1", EntityType.LEAD, triggerMock, userMock, workflowActions, null))
        .isInstanceOf(InvalidWorkflowPropertyException.class);
  }

}