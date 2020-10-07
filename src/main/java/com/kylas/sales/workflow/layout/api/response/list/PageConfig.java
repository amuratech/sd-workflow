package com.kylas.sales.workflow.layout.api.response.list;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class PageConfig {
  private ActionConfig actionConfig;
  private TableConfig tableConfig;

  @JsonCreator
  public PageConfig(
      @JsonProperty("actionConfig") ActionConfig actionConfig,
      @JsonProperty("tableConfig") TableConfig tableConfig) {
    this.actionConfig = actionConfig;
    this.tableConfig = tableConfig;
  }
}
