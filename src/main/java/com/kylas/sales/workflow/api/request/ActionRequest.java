package com.kylas.sales.workflow.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
public class ActionRequest {

  private final ActionType type;
  private final EditPropertyRequest payload;
}
