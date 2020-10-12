package com.kylas.sales.workflow.domain.exception;

import com.kylas.sales.workflow.error.DomainException;
import com.kylas.sales.workflow.error.ErrorCode;

public class InvalidWorkflowPropertyException extends DomainException {

  public InvalidWorkflowPropertyException() {
    super(ErrorCode.INVALID_WORKFLOW_PROPERTY);
  }
}
