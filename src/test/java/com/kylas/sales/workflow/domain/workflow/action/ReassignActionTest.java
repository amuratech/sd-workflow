package com.kylas.sales.workflow.domain.workflow.action;

import static com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType.REASSIGN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kylas.sales.workflow.common.dto.ActionDetail.ReassignAction;
import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.domain.exception.InvalidActionException;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ReassignActionTest {

  @Test
  public void givenReassignAction_withValidValue_shouldCreateIt() {
    //given
    var reassignAction = new ActionResponse(REASSIGN, new ReassignAction(2000L));
    Set<ActionResponse> reassignActions = Set.of(reassignAction);
    //when
    Set<AbstractWorkflowAction> actualReassignActions =
        reassignActions
            .stream()
            .map(actionResponse -> reassignAction.getType().create(actionResponse))
            .collect(Collectors.toSet());
    //then
    assertThat(actualReassignActions).hasSize(1);
    com.kylas.sales.workflow.domain.workflow.action.reassign.ReassignAction reassignActionDomain = (com.kylas.sales.workflow.domain.workflow.action.reassign.ReassignAction) actualReassignActions
        .iterator().next();
    assertThat(reassignActionDomain.getOwnerId()).isEqualTo(2000L);
  }

  @Test
  public void givenReassignAction_withInValidValue_shouldNotCreateItAndThrow() {
    //given
    var reassignAction = new ActionResponse(REASSIGN, new ReassignAction(null));
    //when
    //then
    assertThatThrownBy(() -> com.kylas.sales.workflow.domain.workflow.action.reassign.ReassignAction.createNew(reassignAction))
        .isInstanceOf(InvalidActionException.class);
  }

  @Test
  public void givenReassignAction_withValidValue_shouldUpdateIt() {
    //given
    var reassignActionResponse = new ActionResponse(REASSIGN, new ReassignAction(2000L));
    ReassignAction payload = (ReassignAction) reassignActionResponse.getPayload();
    //when
    com.kylas.sales.workflow.domain.workflow.action.reassign.ReassignAction reassignAction = (com.kylas.sales.workflow.domain.workflow.action.reassign.ReassignAction) new com.kylas.sales.workflow.domain.workflow.action.reassign.ReassignAction()
        .update(reassignActionResponse);
    //then
    assertThat(payload.getOwnerId()).isEqualTo(reassignAction.getOwnerId());
  }

  @Test
  public void givenReassignAction_withInValidValue_shouldNotUpdatedItAndThrow() {
    //given
    var reassignActionResponse = new ActionResponse(REASSIGN, new ReassignAction(null));
    ReassignAction payload = (ReassignAction) reassignActionResponse.getPayload();
    //when
    //then
    assertThatThrownBy(() -> new com.kylas.sales.workflow.domain.workflow.action.reassign.ReassignAction()
        .update(reassignActionResponse))
        .isInstanceOf(InvalidActionException.class);
  }
}
