package com.kylas.sales.workflow.domain.workflow.action;

import com.kylas.sales.workflow.domain.workflow.Workflow;

public interface WorkflowAction {

  void setWorkflow(Workflow workflow);

  enum ActionType {
    EDIT_PROPERTY
  }
}
