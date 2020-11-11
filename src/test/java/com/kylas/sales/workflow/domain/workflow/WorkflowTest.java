package com.kylas.sales.workflow.domain.workflow;

import static com.kylas.sales.workflow.domain.workflow.EntityType.LEAD;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;

import com.kylas.sales.workflow.common.dto.WorkflowAction;
import com.kylas.sales.workflow.common.dto.WorkflowEditProperty;
import com.kylas.sales.workflow.domain.exception.InsufficientPrivilegeException;
import com.kylas.sales.workflow.domain.exception.InvalidWorkflowPropertyException;
import com.kylas.sales.workflow.domain.user.Action;
import com.kylas.sales.workflow.domain.user.Permission;
import com.kylas.sales.workflow.domain.user.User;
import com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType;
import java.util.HashSet;
import java.util.Set;
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
        () -> Workflow.createNew("Workflow1", "Workflow1", LEAD, triggerMock, userMock, workflowActions, conditionMock, true))
        .isInstanceOf(InsufficientPrivilegeException.class);
  }

  @Test
  public void givenUserWithoutHavingUpdateAllPermission_tryToUpdate_shouldThrow() {
    //given
    WorkflowTrigger trigger = WorkflowTrigger
        .createNew(new com.kylas.sales.workflow.common.dto.WorkflowTrigger(TriggerType.EVENT, TriggerFrequency.CREATED));

    var permissionAction = new Action();
    permissionAction.setWrite(true);
    var permission = new Permission(1L, "workflow", "workflow", permissionAction);
    var user = new User(1000L, 999L, Set.of(permission));
    var condition = WorkflowCondition.createNew(new com.kylas.sales.workflow.common.dto.WorkflowCondition(ConditionType.FOR_ALL));
    var actions = singleton(
        EditPropertyAction.createNew(
            new WorkflowAction(ActionType.EDIT_PROPERTY, new WorkflowEditProperty("key", "value"))));
    var workflow = Workflow
        .createNew("Workflow 1", "First Workflow", LEAD, trigger, user, actions, condition, true);
    //when & then

    assertThatExceptionOfType(InsufficientPrivilegeException.class)
        .isThrownBy(() -> workflow.update("Workflow 1", "First Workflow", LEAD, condition, trigger, actions, user));
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
    Workflow aNew = Workflow.createNew("Workflow1", "Workflow1", LEAD, triggerMock, userMock, workflowActions, conditionMock, true);
    //then
    assertThat(aNew).isNotNull();
  }

  @Test
  public void givenWorkflowUpdateRequest_shouldUpdateIt() {
    //given
    WorkflowTrigger trigger = WorkflowTrigger
        .createNew(new com.kylas.sales.workflow.common.dto.WorkflowTrigger(TriggerType.EVENT, TriggerFrequency.CREATED));

    var permissionAction = new Action();
    permissionAction.setWrite(true);
    permissionAction.setUpdateAll(true);
    var permission = new Permission(1L, "workflow", "workflow", permissionAction);
    var user = new User(1000L, 999L, Set.of(permission));
    var actions = singleton(
        EditPropertyAction.createNew(
            new WorkflowAction(ActionType.EDIT_PROPERTY, new WorkflowEditProperty("key", "value"))));
    var condition = WorkflowCondition.createNew(new com.kylas.sales.workflow.common.dto.WorkflowCondition(ConditionType.FOR_ALL));
    var workflow = Workflow
        .createNew("Workflow 1", "First Workflow", LEAD, trigger, user, actions, condition, true);
    //when
    Workflow updatedWorkflow = workflow.update("Workflow 2", "desc2", LEAD, condition, trigger, actions, user);
    //then
    assertThat(updatedWorkflow).isNotNull();
    assertThat(updatedWorkflow.getName()).isEqualTo("Workflow 2");
    assertThat(updatedWorkflow.getWorkflowExecutedEvent().getTriggerCount()).isZero();
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
  public void tryToUpdateWorkflow_withInvalidEntity_shouldThrow() {
    //given

    var permissionAction = new Action();
    permissionAction.setWrite(true);
    permissionAction.setUpdateAll(true);
    var permission = new Permission(1L, "workflow", "workflow", permissionAction);
    var user = new User(1000L, 999L, Set.of(permission));
    WorkflowTrigger trigger = WorkflowTrigger
        .createNew(new com.kylas.sales.workflow.common.dto.WorkflowTrigger(TriggerType.EVENT, TriggerFrequency.CREATED));
    var actions = singleton(
        EditPropertyAction.createNew(
            new WorkflowAction(ActionType.EDIT_PROPERTY, new WorkflowEditProperty("key", "value"))));
    var condition = WorkflowCondition.createNew(new com.kylas.sales.workflow.common.dto.WorkflowCondition(ConditionType.FOR_ALL));
    var workflow = Workflow
        .createNew("Workflow 1", "First Workflow", LEAD, trigger, user, actions, condition, true);
    //when & then
    assertThatExceptionOfType(InvalidWorkflowPropertyException.class)
        .isThrownBy(() -> workflow.update("Workflow 1", "First Workflow", null, condition, trigger, actions, user));
  }

  @Test
  public void tryToCreateWorkflow_withInvalidTrigger_shouldThrow() {
    //given
    User userMock = Mockito.mock(User.class);
    given(userMock.canCreateWorkflow()).willReturn(true);
    HashSet workflowActions = Mockito.mock(HashSet.class);
    WorkflowCondition conditionMock = Mockito.mock(WorkflowCondition.class);
    //when & then
    Assertions.assertThatThrownBy(() -> Workflow.createNew("Workflow1", "Workflow1", LEAD, null, userMock, workflowActions, conditionMock,
        true))
        .isInstanceOf(InvalidWorkflowPropertyException.class);
  }

  @Test
  public void tryToUpdateWorkflow_withInvalidTrigger_shouldThrow() {
    //given

    var permissionAction = new Action();
    permissionAction.setWrite(true);
    permissionAction.setUpdateAll(true);
    var permission = new Permission(1L, "workflow", "workflow", permissionAction);
    var user = new User(1000L, 999L, Set.of(permission));
    WorkflowTrigger trigger = WorkflowTrigger
        .createNew(new com.kylas.sales.workflow.common.dto.WorkflowTrigger(TriggerType.EVENT, TriggerFrequency.CREATED));
    var actions = singleton(
        EditPropertyAction.createNew(
            new WorkflowAction(ActionType.EDIT_PROPERTY, new WorkflowEditProperty("key", "value"))));
    var condition = WorkflowCondition.createNew(new com.kylas.sales.workflow.common.dto.WorkflowCondition(ConditionType.FOR_ALL));
    var workflow = Workflow
        .createNew("Workflow 1", "First Workflow", LEAD, trigger, user, actions, condition, true);
    //when & then
    assertThatExceptionOfType(InvalidWorkflowPropertyException.class)
        .isThrownBy(() -> workflow.update("Workflow 1", "First Workflow", LEAD, condition, null, actions, user));
  }

  @Test
  public void tryToCreateWorkflow_withInvalidAction_shouldThrow() {
    //given
    WorkflowTrigger triggerMock = Mockito.mock(WorkflowTrigger.class);
    User userMock = Mockito.mock(User.class);
    given(userMock.canCreateWorkflow()).willReturn(true);
    WorkflowCondition conditionMock = Mockito.mock(WorkflowCondition.class);
    //when & then
    Assertions.assertThatThrownBy(() -> Workflow.createNew("Workflow1", "Workflow1", LEAD, triggerMock, userMock, null, conditionMock,
        true))
        .isInstanceOf(InvalidWorkflowPropertyException.class);
  }

  @Test
  public void tryToUpdateWorkflow_withInvalidAction_shouldThrow() {
    //given
    var permissionAction = new Action();
    permissionAction.setWrite(true);
    permissionAction.setUpdateAll(true);
    var permission = new Permission(1L, "workflow", "workflow", permissionAction);
    var user = new User(1000L, 999L, Set.of(permission));
    WorkflowTrigger trigger = WorkflowTrigger
        .createNew(new com.kylas.sales.workflow.common.dto.WorkflowTrigger(TriggerType.EVENT, TriggerFrequency.CREATED));
    var condition = WorkflowCondition.createNew(new com.kylas.sales.workflow.common.dto.WorkflowCondition(ConditionType.FOR_ALL));
    var actions = singleton(
        EditPropertyAction.createNew(
            new WorkflowAction(ActionType.EDIT_PROPERTY, new WorkflowEditProperty("key", "value"))));
    var workflow = Workflow
        .createNew("Workflow 1", "First Workflow", LEAD, trigger, user, actions, condition, true);
    //when & then
    assertThatExceptionOfType(InvalidWorkflowPropertyException.class)
        .isThrownBy(() -> workflow.update("Workflow 1", "First Workflow", LEAD, condition, trigger, emptySet(), user));
  }

  @Test
  public void tryToCreateWorkflow_withInvalidCondition_shouldThrow() {
    //given
    WorkflowTrigger triggerMock = Mockito.mock(WorkflowTrigger.class);
    User userMock = Mockito.mock(User.class);
    given(userMock.canCreateWorkflow()).willReturn(true);
    HashSet workflowActions = Mockito.mock(HashSet.class);
    //when & then
    Assertions.assertThatThrownBy(() -> Workflow.createNew("Workflow1", "Workflow1", LEAD, triggerMock, userMock, workflowActions, null,
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
    Workflow workflow = Workflow.createNew("Workflow1", "Workflow1", LEAD, triggerMock, userMock, workflowActions, conditionMock, true)
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
    Workflow workflow = Workflow.createNew("Workflow1", "Workflow1", LEAD, triggerMock, userMock, workflowActions, conditionMock, true)
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
    Workflow workflow = Workflow.createNew("Workflow1", "Workflow1", LEAD, triggerMock, userMock, workflowActions, conditionMock, true)
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
    Workflow workflow = Workflow.createNew("Workflow1", "Workflow1", LEAD, triggerMock, userMock, workflowActions, conditionMock, true)
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
    Workflow workflow = Workflow.createNew("Workflow1", "Workflow1", LEAD, triggerMock, creatorMock, workflowActions, conditionMock, true)
        .setAllowedActionsForUser(readerMock);
    //then
    assertThat(workflow.getAllowedActions().canRead()).isFalse();
    assertThat(workflow.getAllowedActions().canReadAll()).isFalse();
    assertThat(workflow.getAllowedActions().canWrite()).isFalse();
  }
}