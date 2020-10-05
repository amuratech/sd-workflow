package com.kylas.sales.workflow.domain.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Permission implements Serializable {
  private final long id;
  private final String name;
  private final String description;
  private final Action action;

  @JsonCreator
  public Permission(
      @JsonProperty("id") long id,
      @JsonProperty("name") String name,
      @JsonProperty("description") String description,
      @JsonProperty("action") Action action) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.action = action;
  }
}
