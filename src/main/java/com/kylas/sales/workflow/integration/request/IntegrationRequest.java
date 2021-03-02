package com.kylas.sales.workflow.integration.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class IntegrationRequest {

  private final String hookUrl;

  @JsonCreator
  public IntegrationRequest(@JsonProperty("hookUrl") String hookUrl) {
    this.hookUrl = hookUrl;
  }
}
