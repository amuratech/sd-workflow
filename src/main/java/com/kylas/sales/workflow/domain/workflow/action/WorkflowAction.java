package com.kylas.sales.workflow.domain.workflow.action;

import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.domain.workflow.Workflow;

public interface WorkflowAction {

  void setWorkflow(Workflow workflow);

  enum ActionType {
    EDIT_PROPERTY {
      @Override
      public AbstractWorkflowAction create(ActionResponse actionResponse) {
        return EditPropertyAction.createNew(actionResponse);
      }

      @Override
      public ActionResponse toActionResponse(AbstractWorkflowAction workflowAction) {
        return EditPropertyAction.toActionResponse((EditPropertyAction) workflowAction);
      }
    };

    public abstract AbstractWorkflowAction create(ActionResponse action);

    public abstract ActionResponse toActionResponse(AbstractWorkflowAction workflowAction);
  }
}
