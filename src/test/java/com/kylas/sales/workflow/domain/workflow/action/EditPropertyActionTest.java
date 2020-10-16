package com.kylas.sales.workflow.domain.workflow.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kylas.sales.workflow.api.request.ActionRequest;
import com.kylas.sales.workflow.api.request.EditPropertyRequest;
import com.kylas.sales.workflow.domain.exception.InvalidActionException;
import com.kylas.sales.workflow.domain.processor.exception.WorkflowExecutionException;
import com.kylas.sales.workflow.domain.processor.lead.Lead;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType;
import java.util.HashSet;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class EditPropertyActionTest {

  @Test
  public void givenEditPropertyAction_shouldCreate() {
    //given
    var editPropertyAction = new ActionRequest(ActionType.EDIT_PROPERTY, new EditPropertyRequest("firstName", "Tony"));
    Set<ActionRequest> actionRequests = new HashSet<>();
    actionRequests.add(editPropertyAction);
    //when
    Set<AbstractWorkflowAction> actions = EditPropertyAction.createNew(actionRequests);
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

  @Test
  public void givenEntityToUpdateProperty_shouldUpdate() {
    //given
    Lead lead = new Lead();
    lead.setFirstName("Steve");
    lead.setLastName("Stark");
    var editPropertyAction = new ActionRequest(ActionType.EDIT_PROPERTY, new EditPropertyRequest("firstName", "Tony"));
    Set<ActionRequest> actionRequests = new HashSet<>();
    actionRequests.add(editPropertyAction);
    AbstractWorkflowAction workflowAction = EditPropertyAction.createNew(actionRequests).iterator().next();
    //when
    Lead processedLead = (Lead) workflowAction.process(lead);
    //then
    assertThat(processedLead.getFirstName()).isEqualTo("Tony");
    assertThat(processedLead.getLastName()).isEqualTo("Stark");

  }

  @Test
  public void givenEntityToUpdateProperty_shouldThrow() {
    //given
    Lead lead = new Lead();
    lead.setFirstName("Steve");
    lead.setLastName("Stark");
    var editPropertyAction = new ActionRequest(ActionType.EDIT_PROPERTY, new EditPropertyRequest("myFirstName", "Tony"));
    Set<ActionRequest> actionRequests = new HashSet<>();
    actionRequests.add(editPropertyAction);
    AbstractWorkflowAction workflowAction = EditPropertyAction.createNew(actionRequests).iterator().next();
    //when
    assertThatThrownBy(() -> workflowAction.process(lead))
        .isInstanceOf(WorkflowExecutionException.class)
        .hasMessage("01701004");

  }
}