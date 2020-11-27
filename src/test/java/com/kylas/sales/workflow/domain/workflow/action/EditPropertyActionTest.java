package com.kylas.sales.workflow.domain.workflow.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction;
import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.domain.exception.InvalidActionException;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class EditPropertyActionTest {

  @Test
  public void givenEditPropertyAction_shouldCreate() {
    //given
    var editPropertyAction = new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("firstName", "Tony"));
    Set<ActionResponse> actionResponses = new HashSet<>();
    actionResponses.add(editPropertyAction);
    //when
    Set<AbstractWorkflowAction> actions = actionResponses.stream()
        .map(actionResponse -> editPropertyAction.getType().create(actionResponse))
        .collect(Collectors.toSet());
    //then
    assertThat(actions.size()).isEqualTo(1);
    com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction action = (com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction) actions
        .iterator().next();
    assertThat(action.getName()).isEqualTo("firstName");
    assertThat(action.getValue()).isEqualTo("Tony");
  }

  @Test
  public void givenActionWithoutName_shouldThrow() {
    //given
    var editPropertyAction = new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("", "Tony"));
    //when
    //then
    assertThatThrownBy(() -> com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction.createNew(editPropertyAction))
        .isInstanceOf(InvalidActionException.class);
  }

  @Test
  public void givenActionWithoutValue_shouldThrow() {
    //given
    var editPropertyAction = new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("firstName", ""));
    //when
    //then
    assertThatThrownBy(() -> com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction.createNew(editPropertyAction))
        .isInstanceOf(InvalidActionException.class);
  }

}