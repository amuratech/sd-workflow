package com.kylas.sales.workflow.domain.exception;

import com.kylas.sales.workflow.error.DomainException;

public class IntegrationPermissionException extends DomainException {

  public IntegrationPermissionException() {
    super("You are not authorised to create/update an integration-based workflow.");
  }
}
