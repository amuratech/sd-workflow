package com.kylas.sales.workflow.domain.exception;

import com.kylas.sales.workflow.error.DomainException;
import com.kylas.sales.workflow.error.ErrorCode;

public class InvalidConditionException extends DomainException {

  public InvalidConditionException() {
    super(ErrorCode.INVALID_CONDITION);
  }
}
