package com.kylas.sales.workflow.domain.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class EntityDefinition {

  private final String name;
  private final String displayName;

  @JsonCreator
  public EntityDefinition(@JsonProperty("name") String name, @JsonProperty("displayName") String displayName) {
    this.name = name;
    this.displayName = displayName;
  }
}
