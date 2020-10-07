package com.kylas.sales.workflow.layout.api.response.list;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class TableConfig {
  private final String fetchURL;
  private final String searchService;
  private final String recordClickAction;
  private String clickActionUrl;
  private final List<Column> columns;

  @JsonCreator
  public TableConfig(
      @JsonProperty("fetchURL") String fetchURL,
      @JsonProperty("searchService") String searchService,
      @JsonProperty("recordClickAction") String recordClickAction,
      @JsonProperty("columns") List<Column> columns) {
    this.fetchURL = fetchURL;
    this.searchService = searchService;
    this.recordClickAction = recordClickAction;
    this.columns = columns;
  }
}
