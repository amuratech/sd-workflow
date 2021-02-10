package com.kylas.sales.workflow.mq.event;

import com.kylas.sales.workflow.common.dto.UsageRecord;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TenantUsageEvent {

  private final List<UsageRecord> usageRecords;

  public static String getEventName() {
    return "tenant.usage.collected";
  }
}