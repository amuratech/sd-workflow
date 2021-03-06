package com.kylas.sales.workflow.common.dto.condition;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExpressionField {
  PIPELINE("pipeline", "pipeline.id"),
  PIPELINE_STAGE("pipelineStage", "pipelineStage.id"),
  PRODUCT("product", "product.id"),
  PRODUCTS("products", "id"),
  SOURCE("source", "source.id"),
  CAMPAIGN("campaign", "campaign.id"),
  SALUTATION("salutation", "salutation.id"),
  CREATED_BY("createdBy", "createdBy.id"),
  UPDATED_BY("updatedBy", "updatedBy.id"),
  CONVERTED_BY("convertedBy", "convertedBy.id"),
  OWNER_ID("ownerId", "ownerId.id"),
  OWNED_BY("ownedBy", "ownedBy.id"),
  COMPANY("company", "company.id"),
  ASSOCIATED_CONTACTS("associatedContacts", "id"),
  ESTIMATED_VALUE("estimatedValue", "estimatedValue.value"),
  ACTUAL_VALUE("actualValue", "actualValue.value");


  private final String name;
  private final String fieldName;

  public static String getFieldByName(String name) {
    return Arrays.stream(values())
        .filter(expressionField -> expressionField.getName().equals(name))
        .findFirst()
        .map(ExpressionField::getFieldName)
        .orElse(name);
  }
}
