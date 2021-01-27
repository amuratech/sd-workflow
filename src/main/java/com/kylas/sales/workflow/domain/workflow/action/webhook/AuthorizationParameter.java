package com.kylas.sales.workflow.domain.workflow.action.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorizationParameter {

  private final String token;
  private final String keyName;
  private final String value;
  private final String username;
  private final String password;
}
