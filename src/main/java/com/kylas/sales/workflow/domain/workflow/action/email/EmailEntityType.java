package com.kylas.sales.workflow.domain.workflow.action.email;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EmailEntityType {
  LEAD("lead"),
  CONTACT("contact"),
  DEAL("deal"),
  USER("user"),
  EMAIL("email");

  private String entityName;
}
