package com.kylas.sales.workflow.layout.api.response.list;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Column {
  private final String id;
  private final String header;
  @JsonProperty("isStandard") private final boolean standard;
  @JsonProperty("isFilterable") private final boolean filterable;
  @JsonProperty("isSortable") private final boolean sortable;
  @JsonProperty("isInternal") private final boolean internal;
  @JsonProperty("multiValue") private final boolean multiValue;
  @JsonProperty("showDefaultOptions") private final boolean showDefaultOptions;
  private final FieldType fieldType;
  private Object picklist;
  private final Lookup lookup;
  private final String values;

  @JsonCreator
  public Column(
      @JsonProperty("id") String id,
      @JsonProperty("header") String header,
      @JsonProperty("isStandard") boolean standard,
      @JsonProperty("isFilterable") boolean filterable,
      @JsonProperty("isSortable") boolean sortable,
      @JsonProperty("isInternal") boolean internal,
      @JsonProperty("multiValue") boolean multiValue,
      @JsonProperty("fieldType") FieldType fieldType,
      @JsonProperty("picklist") Object picklist,
      @JsonProperty("lookup") Lookup lookup,
      @JsonProperty("values") String values,
      @JsonProperty("showDefaultOptions") boolean showDefaultOptions) {
    this.id = id;
    this.header = header;
    this.standard = standard;
    this.filterable = filterable;
    this.sortable = sortable;
    this.internal = internal;
    this.multiValue = multiValue;
    this.fieldType = fieldType;
    this.picklist = picklist;
    this.lookup = lookup;
    this.values=values;
    this.showDefaultOptions = showDefaultOptions;
  }

}
