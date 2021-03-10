package com.kylas.sales.workflow.domain.processor.email;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Participant {

  private final String entity;
  private final Long id;
  private final String name;
  private final String email;

  @JsonCreator
  public Participant(@JsonProperty("entity") String entity, @JsonProperty("id") Long id,
      @JsonProperty("name") String name, @JsonProperty("email") String email) {
    this.entity = entity;
    this.id = id;
    this.name = name;
    this.email = email;
  }
}
