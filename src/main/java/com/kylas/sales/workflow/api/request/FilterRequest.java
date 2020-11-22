package com.kylas.sales.workflow.api.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.ToString;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class FilterRequest {

  private final ArrayList<Filter> filters = new ArrayList<>();

  @JsonCreator
  public FilterRequest(@JsonProperty("rules") List<Filter> filters) {
    this.filters.addAll(filters);
  }

  @Getter
  @ToString
  public static class Filter {
    private final String operator;
    private final String fieldName;
    private final String fieldType;
    private final Object value;

    @JsonCreator
    public Filter(
        @JsonProperty("operator") String operator,
        @JsonProperty("field") String fieldName,
        @JsonProperty("type") String fieldType,
        @JsonProperty("value") Object value) {
      this.operator = operator;
      this.fieldName = fieldName;
      this.fieldType = fieldType;
      this.value = value;
    }
  }
}
