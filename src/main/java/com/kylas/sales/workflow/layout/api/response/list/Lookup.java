package com.kylas.sales.workflow.layout.api.response.list;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Lookup {
  private final String entity;
  private final String lookupUrl;

  @JsonCreator
  public Lookup(
      @JsonProperty("entity") String entity, @JsonProperty("lookupUrl") String lookupUrl) {
    this.entity = entity;
    this.lookupUrl = lookupUrl;
  }
}
