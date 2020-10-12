package com.kylas.sales.workflow.api.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kylas.sales.workflow.domain.workflow.ConditionType;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConditionRequest {
  private ConditionType conditionType;

  @JsonCreator
  public ConditionRequest(@JsonProperty("conditionType") ConditionType conditionType) {
    this.conditionType = conditionType;
  }
}
