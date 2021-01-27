package com.kylas.sales.workflow.common.dto;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class User {

  private final long id;
  private final String name;

  @JsonCreator
  public User(@JsonProperty("id") long id, @JsonProperty("name") String name) {
    this.id = id;
    this.name = name;
  }
}
