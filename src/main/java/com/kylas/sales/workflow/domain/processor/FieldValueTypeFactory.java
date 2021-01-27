package com.kylas.sales.workflow.domain.processor;

import com.kylas.sales.workflow.domain.processor.FieldValueType.DealFieldValueType;
import com.kylas.sales.workflow.domain.processor.FieldValueType.LeadFieldValueType;
import com.kylas.sales.workflow.domain.workflow.EntityType;

public class FieldValueTypeFactory {

  public static FieldValueType createByEntityType(EntityType entityType) {
    switch (entityType) {
      case LEAD:
        return new LeadFieldValueType();
      case DEAL:
        return new DealFieldValueType();
      default:
        break;
    }
    return new LeadFieldValueType();
  }
}
