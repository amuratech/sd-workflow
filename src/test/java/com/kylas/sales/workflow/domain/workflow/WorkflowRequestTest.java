package com.kylas.sales.workflow.domain.workflow;

import static com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.ValueType.PLAIN;
import static com.kylas.sales.workflow.domain.workflow.ConditionType.FOR_ALL;
import static com.kylas.sales.workflow.domain.workflow.EntityType.DEAL;
import static com.kylas.sales.workflow.domain.workflow.EntityType.LEAD;
import static com.kylas.sales.workflow.domain.workflow.EntityType.USER;
import static com.kylas.sales.workflow.domain.workflow.TriggerFrequency.CREATED;
import static com.kylas.sales.workflow.domain.workflow.TriggerType.EVENT;
import static com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType.EDIT_PROPERTY;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.kylas.sales.workflow.common.dto.ActionDetail;
import com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction;
import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import com.kylas.sales.workflow.domain.WorkflowFacade;
import com.kylas.sales.workflow.domain.exception.InvalidActionException;
import com.kylas.sales.workflow.domain.exception.InvalidValueTypeException;
import com.kylas.sales.workflow.domain.exception.InvalidWorkflowRequestException;
import com.kylas.sales.workflow.stubs.WorkflowStub;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(initializers = {TestDatabaseInitializer.class})
class WorkflowRequestTest {

  @Autowired
  WorkflowFacade workflowFacade;

  @Test
  public void givenWorkflowRequest_withInvalidEntityType_shouldThrow() {
    //given
    var actions = new HashSet<ActionResponse>();
    actions.add(new ActionResponse(EDIT_PROPERTY, new EditPropertyAction("firstName", "test name", PLAIN)));
    actions.add(new ActionResponse(EDIT_PROPERTY, new EditPropertyAction("products", "[{\"id\":26,\"name\":\"Marketing Service\"}]", PLAIN)));
    var workflowRequest = WorkflowStub
        .aWorkflowRequestWithActions("Workflow 1", "Workflow Description", USER, EVENT, CREATED, FOR_ALL, true, actions);
    //when
    //then
    assertThatExceptionOfType(InvalidWorkflowRequestException.class).isThrownBy(() -> workflowFacade.validate(workflowRequest));
  }

  @Test
  public void givenWorkflowRequest_withoutActions_shouldThrow() {
    //given
    var actions = new HashSet<ActionResponse>();
    var workflowRequest = WorkflowStub
        .aWorkflowRequestWithActions("Workflow 1", "Workflow Description", USER, EVENT, CREATED, FOR_ALL, true, actions);
    //when
    //then
    assertThatExceptionOfType(InvalidWorkflowRequestException.class).isThrownBy(() -> workflowFacade.validate(workflowRequest));
  }

  @Test
  public void givenWorkflowRequest_withInvalidPropertyValue_shouldThrow() {
    //given
    Set<ActionResponse> actions = Set.of(
        new ActionResponse(EDIT_PROPERTY, new ActionDetail.EditPropertyAction("city", null, PLAIN)));

    var workflowRequest = WorkflowStub
        .aWorkflowRequestWithActions("Edit Lead Property", "Edit Lead Property", EntityType.LEAD, TriggerType.EVENT, CREATED,
            ConditionType.FOR_ALL, true, actions);
    //then
    assertThatExceptionOfType(InvalidActionException.class)
        .isThrownBy(() -> workflowFacade.validate(workflowRequest));
  }


  @Test
  public void givenWorkflowRequest_withEntityLeadAndInvalidValueTypes_shouldThrow() {
    //given
    var actions = new HashSet<ActionResponse>();
    actions.add(new ActionResponse(EDIT_PROPERTY, new EditPropertyAction("firstName", "test name", PLAIN)));
    actions.add(new ActionResponse(EDIT_PROPERTY, new EditPropertyAction("products", "[{\"id\":26,\"name\":\"Marketing Service\"}]", PLAIN)));
    var workflowRequest = WorkflowStub
        .aWorkflowRequestWithActions("Workflow 1", "Workflow Description", LEAD, EVENT, CREATED, FOR_ALL, true, actions);
    //when
    //then
    assertThatExceptionOfType(InvalidValueTypeException.class).isThrownBy(() -> workflowFacade.validate(workflowRequest));
  }

  @Test
  public void givenWorkflowRequest_withEntityDealAndInvalidValueTypes_shouldThrow() {
    //given
    var actions = new HashSet<ActionResponse>();
    actions.add(new ActionResponse(EDIT_PROPERTY, new EditPropertyAction("name", "test name", PLAIN)));
    actions.add(new ActionResponse(EDIT_PROPERTY, new EditPropertyAction("product", "{\"id\":26,\"name\":\"Marketing Service\"}", PLAIN)));
    actions.add(new ActionResponse(EDIT_PROPERTY, new EditPropertyAction("company", "{\"id\":26,\"name\":\"Marketing Service Org\"}", PLAIN)));
    var workflowRequest = WorkflowStub
        .aWorkflowRequestWithActions("Workflow 1", "Workflow Description", DEAL, EVENT, CREATED, FOR_ALL, true, actions);
    //when
    //then
    assertThatExceptionOfType(InvalidValueTypeException.class).isThrownBy(() -> workflowFacade.validate(workflowRequest));
  }
}
