package com.kylas.sales.workflow.domain.workflow.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kylas.sales.workflow.api.request.ActionRequest;
import com.kylas.sales.workflow.api.request.EditPropertyRequest;
import com.kylas.sales.workflow.domain.exception.InvalidActionException;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EditPropertyActionTest {

  @Test
  public void givenEditPropertyAction_shouldCreate() {
    //given
    var editPropertyAction = new ActionRequest(ActionType.EDIT_PROPERTY, new EditPropertyRequest("firstName", "Tony"));
    Set<ActionRequest> actionRequests = new HashSet<>();
    actionRequests.add(editPropertyAction);
    //when
    Set<WorkflowAction> actions = EditPropertyAction.createNew(actionRequests);
    //then
    assertThat(actions.size()).isEqualTo(1);
    EditPropertyAction action = (EditPropertyAction) actions.iterator().next();
    assertThat(action.getName()).isEqualTo("firstName");
    assertThat(action.getValue()).isEqualTo("Tony");
  }

  @Test
  public void givenActionWithoutName_shouldThrow() {
    //given
    var editPropertyAction = new ActionRequest(ActionType.EDIT_PROPERTY, new EditPropertyRequest("", "Tony"));
    Set<ActionRequest> actionRequests = new HashSet<>();
    actionRequests.add(editPropertyAction);
    //when
    //then
    assertThatThrownBy(() -> EditPropertyAction.createNew(actionRequests))
        .isInstanceOf(InvalidActionException.class);
  }

  @Test
  public void givenActionWithoutValue_shouldThrow() {
    //given
    var editPropertyAction = new ActionRequest(ActionType.EDIT_PROPERTY, new EditPropertyRequest("firstName", ""));
    Set<ActionRequest> actionRequests = new HashSet<>();
    actionRequests.add(editPropertyAction);
    //when
    //then
    assertThatThrownBy(() -> EditPropertyAction.createNew(actionRequests))
        .isInstanceOf(InvalidActionException.class);
  }
}