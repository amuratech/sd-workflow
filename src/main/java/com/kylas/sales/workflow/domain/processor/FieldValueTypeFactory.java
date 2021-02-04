package com.kylas.sales.workflow.domain.processor;

import com.kylas.sales.workflow.domain.exception.InvalidEntityException;
import com.kylas.sales.workflow.domain.processor.FieldValueType.ContactFieldValueType;
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
      case CONTACT:
        return new ContactFieldValueType();
      default:
        break;
    }
    throw new InvalidEntityException();
  }
}
