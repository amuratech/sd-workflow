package com.kylas.sales.workflow.layout.api.exception;

import com.kylas.sales.workflow.error.ErrorCode;
import com.kylas.sales.workflow.error.ResourceNotFoundException;

public class LayoutNotFoundException extends ResourceNotFoundException {

  public LayoutNotFoundException() {
    super(ErrorCode.LAYOUT_NOT_FOUND);
  }
}
