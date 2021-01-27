package com.kylas.sales.workflow.domain.processor.deal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kylas.sales.workflow.domain.processor.lead.IdName;
import lombok.Getter;

@Getter
public class Pipeline {

  private final Long id;
  private final String name;
  private final IdName stage;

  @JsonCreator
  public Pipeline(@JsonProperty("id") Long id, @JsonProperty("name") String name, @JsonProperty("stage") IdName stage) {
    this.id = id;
    this.name = name;
    this.stage = stage;
  }

}
