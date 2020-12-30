package com.kylas.sales.workflow.domain.processor.lead;

import com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.ValueType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LeadFieldValueType {
  PRODUCTS("products", ValueType.ARRAY),
  PHONE_NUMBERS("phoneNumbers", ValueType.ARRAY),
  EMAILS("emails", ValueType.ARRAY),
  COMPANY_PHONES("companyPhones", ValueType.ARRAY),
  CONVERSION_ASSOCIATION("conversionAssociation", ValueType.OBJECT),
  PIPELINE("pipeline", ValueType.OBJECT),
  OTHER("other", ValueType.PLAIN);

  private final String fieldName;
  private final ValueType valueType;
}
