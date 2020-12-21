package com.kylas.sales.workflow.domain.exception;

import com.kylas.sales.workflow.error.DomainException;
import com.kylas.sales.workflow.error.ErrorCode;

public class InvalidValueTypeException extends DomainException {

  public InvalidValueTypeException() {
    super(ErrorCode.INVALID_VALUE_TYPE);
  }
}
