package com.kylas.sales.workflow.domain.workflow.action.reassign;

import static com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType.REASSIGN;
import static java.util.Objects.isNull;

import com.kylas.sales.workflow.common.dto.ActionDetail;
import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.domain.exception.InvalidActionException;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.action.AbstractWorkflowAction;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction;
import javax.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class ReassignAction extends AbstractWorkflowAction implements WorkflowAction {

  private Long ownerId;

  public ReassignAction(Long ownerId) {
    this.ownerId = ownerId;
  }

  public static AbstractWorkflowAction createNew(ActionResponse actionResponse) {
    var payload = (ActionDetail.ReassignAction) actionResponse.getPayload();
    if (isNull(payload.getOwnerId())) {
      throw new InvalidActionException();
    }
    return new ReassignAction(payload.getOwnerId());
  }

  public static ActionResponse toActionResponse(ReassignAction action) {
    var reassignAction = new ActionDetail.ReassignAction(action.ownerId);
    return new ActionResponse(action.getId(), REASSIGN, reassignAction);
  }

  @Override
  public void setWorkflow(Workflow workflow) {
    super.setWorkflow(workflow);
  }

  @Override
  public AbstractWorkflowAction update(ActionResponse actionResponse) {
    var payload = (ActionDetail.ReassignAction) actionResponse.getPayload();
    if (isNull(payload.getOwnerId())) {
      throw new InvalidActionException();
    }
    this.setOwnerId(payload.getOwnerId());
    return this;
  }

  @Override
  public ActionType getType() {
    return REASSIGN;
  }
}
