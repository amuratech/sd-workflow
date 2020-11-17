package com.kylas.sales.workflow.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class ActionResponse {

  private UUID id;
  private ActionType type;
  private WorkflowEditProperty payload;

  public ActionResponse(ActionType type, WorkflowEditProperty payload) {
    this.type = type;
    this.payload = payload;
  }
}
