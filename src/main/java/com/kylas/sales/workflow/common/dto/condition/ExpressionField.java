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
  SOURCE("source","source.id"),
  CAMPAIGN("campaign","campaign.id"),
  SALUTATION("salutation","salutation.id");


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
