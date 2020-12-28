package com.kylas.sales.workflow.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
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

  @JsonTypeInfo(use = Id.NAME, property = "type", include = As.EXTERNAL_PROPERTY)
  @JsonSubTypes({
      @JsonSubTypes.Type(value = ActionDetail.EditPropertyAction.class, name = "EDIT_PROPERTY"),
      @JsonSubTypes.Type(value = ActionDetail.WebhookAction.class, name = "WEBHOOK"),
      @JsonSubTypes.Type(value = ActionDetail.ReassignAction.class, name = "REASSIGN")
  })
  private ActionDetail payload;

  public ActionResponse(ActionType type, ActionDetail payload) {
    this.type = type;
    this.payload = payload;
  }
}
