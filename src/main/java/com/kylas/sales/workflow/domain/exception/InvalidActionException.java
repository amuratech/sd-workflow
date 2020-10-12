package com.kylas.sales.workflow.domain.exception;

import com.kylas.sales.workflow.error.DomainException;
import com.kylas.sales.workflow.error.ErrorCode;

public class InvalidActionException extends DomainException {

  public InvalidActionException() {
    super(ErrorCode.INVALID_ACTION);
  }
}
