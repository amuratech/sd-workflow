package com.kylas.sales.workflow.domain.workflow;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EntityType {
  LEAD(true), CONTACT(true), DEAL(true), USER(false), TENANT(false), CUSTOM(false);

  private final boolean workflowEntity;
}
