package com.kylas.sales.workflow.domain.workflow.action;

import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.action.email.EmailAction;
import com.kylas.sales.workflow.domain.workflow.action.reassign.ReassignAction;
import com.kylas.sales.workflow.domain.workflow.action.task.CreateTaskAction;
import com.kylas.sales.workflow.domain.workflow.action.webhook.WebhookAction;

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
    },
    WEBHOOK {
      @Override
      public AbstractWorkflowAction create(ActionResponse actionResponse) {
        return WebhookAction.createNew(actionResponse);
      }

      @Override
      public ActionResponse toActionResponse(AbstractWorkflowAction workflowAction) {
        return WebhookAction.toActionResponse((WebhookAction) workflowAction);
      }
    },
    REASSIGN {
      @Override
      public AbstractWorkflowAction create(ActionResponse actionResponse) {
        return ReassignAction.createNew(actionResponse);
      }

      @Override
      public ActionResponse toActionResponse(AbstractWorkflowAction workflowAction) {
        return ReassignAction.toActionResponse((ReassignAction) workflowAction);
      }
    },
    CREATE_TASK {
      @Override
      public AbstractWorkflowAction create(ActionResponse action) {
        return CreateTaskAction.createNew(action);
      }

      @Override
      public ActionResponse toActionResponse(AbstractWorkflowAction workflowAction) {
        return CreateTaskAction.toActionResponse((CreateTaskAction) workflowAction);
      }
    },
    SEND_EMAIL {
      @Override
      public AbstractWorkflowAction create(ActionResponse action) {
        return EmailAction.createNew(action);
      }

      @Override
      public ActionResponse toActionResponse(AbstractWorkflowAction workflowAction) {
        return EmailAction.toActionResponse((EmailAction) workflowAction);
      }
    };

    public abstract AbstractWorkflowAction create(ActionResponse action);

    public abstract ActionResponse toActionResponse(AbstractWorkflowAction workflowAction);
  }
}
