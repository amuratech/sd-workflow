package com.kylas.sales.workflow.domain.workflow.action;

import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.action.reassign.ReassignAction;
import com.kylas.sales.workflow.domain.workflow.action.webhook.WebhookAction;
import reactor.core.publisher.Mono;

public interface WorkflowAction {

  void setWorkflow(Workflow workflow);

  enum ActionType {
    EDIT_PROPERTY {
      @Override
      public AbstractWorkflowAction create(ActionResponse actionResponse) {
        return EditPropertyAction.createNew(actionResponse);
      }

      @Override
      public Mono<ActionResponse> toActionResponse(AbstractWorkflowAction workflowAction, String authenticationToken) {
        return EditPropertyAction.toActionResponse((EditPropertyAction) workflowAction, authenticationToken);
      }
    },
    WEBHOOK {
      @Override
      public AbstractWorkflowAction create(ActionResponse actionResponse) {
        return WebhookAction.createNew(actionResponse);
      }

      @Override
      public Mono<ActionResponse> toActionResponse(AbstractWorkflowAction workflowAction, String authenticationToken) {
        return Mono.just(WebhookAction.toActionResponse((WebhookAction) workflowAction));
      }
    },
    REASSIGN {
      @Override
      public AbstractWorkflowAction create(ActionResponse actionResponse) {
        return ReassignAction.createNew(actionResponse);
      }

      @Override
      public Mono<ActionResponse> toActionResponse(AbstractWorkflowAction workflowAction, String authenticationToken) {
        return ReassignAction.toActionResponse((ReassignAction) workflowAction, authenticationToken);
      }
    };

    public abstract AbstractWorkflowAction create(ActionResponse action);

    public abstract Mono<ActionResponse> toActionResponse(AbstractWorkflowAction workflowAction, String authenticationToken);
  }
}
