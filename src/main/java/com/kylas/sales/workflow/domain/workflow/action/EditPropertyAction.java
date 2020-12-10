package com.kylas.sales.workflow.domain.workflow.action;

import static com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType.EDIT_PROPERTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.kylas.sales.workflow.common.dto.ActionDetail;
import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.domain.exception.InvalidActionException;
import com.kylas.sales.workflow.domain.workflow.Workflow;
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
public class EditPropertyAction extends AbstractWorkflowAction implements com.kylas.sales.workflow.domain.workflow.action.WorkflowAction {

  private String name;
  private String value;

  private EditPropertyAction(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public static AbstractWorkflowAction createNew(ActionResponse actionResponse) {
    var payload = (ActionDetail.EditPropertyAction) actionResponse.getPayload();
    if (isBlank(payload.getName()) || getCharCount(payload.getValue()) < 3) {
      throw new InvalidActionException();
    }
    return new EditPropertyAction(payload.getName(), payload.getValue());
  }

  private static int getCharCount(String value) {
    return isBlank(value) ? 0 : value.length();
  }

  public static ActionResponse toActionResponse(EditPropertyAction action) {
    var editPropertyAction = new ActionDetail.EditPropertyAction(action.name, action.value);
    return new ActionResponse(action.getId(), EDIT_PROPERTY, editPropertyAction);
  }

  @Override
  public void setWorkflow(Workflow workflow) {
    super.setWorkflow(workflow);
  }

  @Override
  public EditPropertyAction update(ActionResponse action) {
    var payload = (ActionDetail.EditPropertyAction) action.getPayload();
    if (isBlank(payload.getName()) || getCharCount(payload.getValue()) < 3) {
      throw new InvalidActionException();
    }
    this.setName(payload.getName());
    this.setValue(payload.getValue());
    return this;
  }

  @Override
  public ActionType getType() {
    return EDIT_PROPERTY;
  }
}
