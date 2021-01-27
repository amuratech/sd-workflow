package com.kylas.sales.workflow.api.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class WorkflowSummary {

  private final long id;

  @JsonCreator
  public WorkflowSummary(@JsonProperty("id") long id) {
    this.id = id;
  }
}
