package com.kylas.sales.workflow.domain.exception;

import com.kylas.sales.workflow.error.DomainException;
import com.kylas.sales.workflow.error.ErrorCode;

public class InvalidEntityException extends DomainException {

  public InvalidEntityException() {
    super(ErrorCode.INVALID_ENTITY);
  }
}
