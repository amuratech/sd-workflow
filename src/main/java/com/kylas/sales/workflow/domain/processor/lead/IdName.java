package com.kylas.sales.workflow.domain.processor.lead;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class IdName {

  private final Long id;
  private final String name;

  @JsonCreator
  public IdName(@JsonProperty("id") Long id, @JsonProperty("name") String name) {
    this.id = id;
    this.name = name;
  }
}
