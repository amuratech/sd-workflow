package com.kylas.sales.workflow.domain.workflow.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kylas.sales.workflow.common.dto.WorkflowAction;
import com.kylas.sales.workflow.common.dto.WorkflowEditProperty;
import com.kylas.sales.workflow.domain.exception.InvalidActionException;
import com.kylas.sales.workflow.domain.processor.exception.WorkflowExecutionException;
import com.kylas.sales.workflow.domain.processor.lead.Lead;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EditPropertyActionTest {

  @Test
  public void givenEditPropertyAction_shouldCreate() {
    //given
    var editPropertyAction = new WorkflowAction(ActionType.EDIT_PROPERTY, new WorkflowEditProperty("firstName", "Tony"));
    Set<WorkflowAction> workflowActions = new HashSet<>();
    workflowActions.add(editPropertyAction);
    //when
    Set<AbstractWorkflowAction> actions = EditPropertyAction.createNew(workflowActions);
    //then
    assertThat(actions.size()).isEqualTo(1);
    EditPropertyAction action = (EditPropertyAction) actions.iterator().next();
    assertThat(action.getName()).isEqualTo("firstName");
    assertThat(action.getValue()).isEqualTo("Tony");
  }

  @Test
  public void givenActionWithoutName_shouldThrow() {
    //given
    var editPropertyAction = new WorkflowAction(ActionType.EDIT_PROPERTY, new WorkflowEditProperty("", "Tony"));
    Set<WorkflowAction> workflowActions = new HashSet<>();
    workflowActions.add(editPropertyAction);
    //when
    //then
    assertThatThrownBy(() -> EditPropertyAction.createNew(workflowActions))
        .isInstanceOf(InvalidActionException.class);
  }

  @Test
  public void givenActionWithoutValue_shouldThrow() {
    //given
    var editPropertyAction = new WorkflowAction(ActionType.EDIT_PROPERTY, new WorkflowEditProperty("firstName", ""));
    Set<WorkflowAction> workflowActions = new HashSet<>();
    workflowActions.add(editPropertyAction);
    //when
    //then
    assertThatThrownBy(() -> EditPropertyAction.createNew(workflowActions))
        .isInstanceOf(InvalidActionException.class);
  }

  @Test
  public void givenEntityToUpdateProperty_shouldUpdate() {
    //given
    Lead lead = new Lead();
    lead.setFirstName("Steve");
    lead.setLastName("Stark");
    var editPropertyAction = new WorkflowAction(ActionType.EDIT_PROPERTY, new WorkflowEditProperty("firstName", "Tony"));
    Set<WorkflowAction> workflowActions = new HashSet<>();
    workflowActions.add(editPropertyAction);
    AbstractWorkflowAction workflowAction = EditPropertyAction.createNew(workflowActions).iterator().next();
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
    var editPropertyAction = new WorkflowAction(ActionType.EDIT_PROPERTY, new WorkflowEditProperty("myFirstName", "Tony"));
    Set<WorkflowAction> workflowActions = new HashSet<>();
    workflowActions.add(editPropertyAction);
    AbstractWorkflowAction workflowAction = EditPropertyAction.createNew(workflowActions).iterator().next();
    //when
    assertThatThrownBy(() -> workflowAction.process(lead))
        .isInstanceOf(WorkflowExecutionException.class)
        .hasMessage("01701004");

  }
}