package com.kylas.sales.workflow.domain.workflow.action.webhook.attribute;

import lombok.Getter;

@Getter
public class Attribute {

  private final String name;
  private final String displayName;

  public Attribute(String name, String displayName) {
    this.name = name;
    this.displayName = displayName;
  }
}
