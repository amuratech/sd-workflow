package com.kylas.sales.workflow.domain.exception;


import com.kylas.sales.workflow.error.ErrorCode;

public class InvalidFilterException extends RuntimeException {

  public InvalidFilterException() {
    super(ErrorCode.INVALID_FILTER_RULE);
  }
}
