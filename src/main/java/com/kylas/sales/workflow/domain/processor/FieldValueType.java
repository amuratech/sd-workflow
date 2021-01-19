package com.kylas.sales.workflow.domain.processor;

import static com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.ValueType.ARRAY;
import static com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.ValueType.OBJECT;
import static com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.ValueType.PLAIN;
import static java.util.Objects.isNull;

import com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.ValueType;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

public interface FieldValueType {

  class LeadFieldValueType implements FieldValueType {

    @Getter
    @AllArgsConstructor
    private enum FieldValueTypes {
      PRODUCTS("products", ARRAY),
      PHONE_NUMBERS("phoneNumbers", ARRAY),
      EMAILS("emails", ARRAY),
      COMPANY_PHONES("companyPhones", ARRAY),
      CONVERSION_ASSOCIATION("conversionAssociation", OBJECT),
      PIPELINE("pipeline", OBJECT),
      OTHER("other", PLAIN);

      private final String fieldName;
      private final ValueType valueType;
    }

    @Override
    public boolean isInValidValueType(String name, ValueType valueType) {
      if (isNull(valueType)) {
        return true;
      }
      FieldValueTypes fieldValueTypes = Arrays
          .stream(FieldValueTypes.values()).filter(value -> value.getFieldName().equals(name)).findAny()
          .orElse(FieldValueTypes.OTHER);
      if (fieldValueTypes.equals(FieldValueTypes.OTHER) && valueType.equals(PLAIN)) {
        return false;
      }
      return !fieldValueTypes.getValueType().equals(valueType);
    }
  }

  class DealFieldValueType implements FieldValueType {

    @Getter
    @AllArgsConstructor
    private enum FieldValueTypes {
      OWNED_BY("ownedBy", OBJECT),
      ESTIMATED_VALUE("estimatedValue", OBJECT),
      ACTUAL_VALUE("actualValue", OBJECT),
      PRODUCT("product", OBJECT),
      PIPELINE("pipeline", OBJECT),
      ASSOCIATED_CONTACTS("associatedContacts", ARRAY),
      COMPANY("company", OBJECT),
      OTHER("other", PLAIN);

      private final String fieldName;
      private final ValueType valueType;
    }

    @Override
    public boolean isInValidValueType(String name, ValueType valueType) {
      if (isNull(valueType)) {
        return true;
      }
      FieldValueTypes fieldValueTypes = Arrays
          .stream(FieldValueTypes.values()).filter(value -> value.getFieldName().equals(name)).findAny()
          .orElse(FieldValueTypes.OTHER);
      if (fieldValueTypes.equals(FieldValueTypes.OTHER) && valueType.equals(PLAIN)) {
        return false;
      }
      return !fieldValueTypes.getValueType().equals(valueType);
    }
  }

  boolean isInValidValueType(String name, ValueType valueType);
}
