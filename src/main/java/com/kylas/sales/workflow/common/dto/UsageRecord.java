package com.kylas.sales.workflow.common.dto;

import lombok.Getter;

@Getter
public class UsageRecord {

  private final Long tenantId;
  private final String usageEntity;
  private final Long count;

  public UsageRecord(Long tenantId, Long count) {
    this.tenantId = tenantId;
    this.usageEntity = "ACTIVE_WORKFLOW";
    this.count = count;
  }
}
