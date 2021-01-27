package com.kylas.sales.workflow.domain.exception;

import com.kylas.sales.workflow.error.DomainException;
import com.kylas.sales.workflow.error.ErrorCode;

public class InvalidWorkflowRequestException extends DomainException {

  public InvalidWorkflowRequestException() {
    super(ErrorCode.INVALID_REQUEST);
  }
}
