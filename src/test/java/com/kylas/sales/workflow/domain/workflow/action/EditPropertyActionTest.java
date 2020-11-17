package com.kylas.sales.workflow.domain.workflow.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.common.dto.WorkflowEditProperty;
import com.kylas.sales.workflow.domain.exception.InvalidActionException;
import com.kylas.sales.workflow.domain.processor.exception.WorkflowExecutionException;
import com.kylas.sales.workflow.domain.processor.lead.Lead;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class EditPropertyActionTest {

  @Test
  public void givenEditPropertyAction_shouldCreate() {
    //given
    var editPropertyAction = new ActionResponse(ActionType.EDIT_PROPERTY, new WorkflowEditProperty("firstName", "Tony"));
    Set<ActionResponse> actionResponses = new HashSet<>();
    actionResponses.add(editPropertyAction);
    //when
    Set<AbstractWorkflowAction> actions = actionResponses.stream()
        .map(actionResponse -> editPropertyAction.getType().create(actionResponse))
        .collect(Collectors.toSet());
    //then
    assertThat(actions.size()).isEqualTo(1);
    EditPropertyAction action = (EditPropertyAction) actions.iterator().next();
    assertThat(action.getName()).isEqualTo("firstName");
    assertThat(action.getValue()).isEqualTo("Tony");
  }

  @Test
  public void givenActionWithoutName_shouldThrow() {
    //given
    var editPropertyAction = new ActionResponse(ActionType.EDIT_PROPERTY, new WorkflowEditProperty("", "Tony"));
    //when
    //then
    assertThatThrownBy(() -> EditPropertyAction.createNew(editPropertyAction))
        .isInstanceOf(InvalidActionException.class);
  }

  @Test
  public void givenActionWithoutValue_shouldThrow() {
    //given
    var editPropertyAction = new ActionResponse(ActionType.EDIT_PROPERTY, new WorkflowEditProperty("firstName", ""));
    //when
    //then
    assertThatThrownBy(() -> EditPropertyAction.createNew(editPropertyAction))
        .isInstanceOf(InvalidActionException.class);
  }

  @Test
  public void givenEntityToUpdateProperty_shouldUpdate() {
    //given
    Lead lead = new Lead();
    lead.setFirstName("Steve");
    lead.setLastName("Stark");
    var editPropertyAction = new ActionResponse(ActionType.EDIT_PROPERTY, new WorkflowEditProperty("firstName", "Tony"));

    AbstractWorkflowAction workflowAction = EditPropertyAction.createNew(editPropertyAction);
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
    var editPropertyAction = new ActionResponse(ActionType.EDIT_PROPERTY, new WorkflowEditProperty("myFirstName", "Tony"));
    AbstractWorkflowAction workflowAction = EditPropertyAction.createNew(editPropertyAction);
    //when
    assertThatThrownBy(() -> workflowAction.process(lead))
        .isInstanceOf(WorkflowExecutionException.class)
        .hasMessage("01701004");

  }
}