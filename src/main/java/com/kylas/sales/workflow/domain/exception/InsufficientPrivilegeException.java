package com.kylas.sales.workflow.domain.exception;


import com.kylas.sales.workflow.error.ErrorCode;

public class InsufficientPrivilegeException extends SecurityException {

  public InsufficientPrivilegeException() {
    super(ErrorCode.INSUFFICIENT_PRIVILEGES);
  }
}
