package com.kylas.sales.workflow.domain.processor.exception;

import com.kylas.sales.workflow.error.DomainException;

public class WorkflowExecutionException extends DomainException {

  public WorkflowExecutionException(String message) {
    super(message);
  }
}
