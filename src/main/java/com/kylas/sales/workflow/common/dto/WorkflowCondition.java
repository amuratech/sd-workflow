package com.kylas.sales.workflow.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kylas.sales.workflow.domain.workflow.ConditionType;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowCondition {
  private ConditionType conditionType;

  @JsonCreator
  public WorkflowCondition(@JsonProperty("conditionType") ConditionType conditionType) {
    this.conditionType = conditionType;
  }
}
