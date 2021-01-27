package com.kylas.sales.workflow.domain.exception;

import com.kylas.sales.workflow.error.ErrorCode;
import com.kylas.sales.workflow.error.ResourceNotFoundException;

public class WorkflowNotFoundException extends ResourceNotFoundException {

  public WorkflowNotFoundException() {
    super(ErrorCode.WORKFLOW_NOT_FOUND);
  }
}
