package com.kylas.sales.workflow.common.dto.condition;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Operator {
  AND,
  OR,
  EQUAL,
  NOT_EQUAL,
  GREATER,
  GREATER_OR_EQUAL,
  LESS,
  LESS_OR_EQUAL,
  IS_NOT_NULL,
  IS_NULL,
  CONTAINS,
  NOT_CONTAINS,
  BETWEEN,
  NOT_BETWEEN,
  IN,
  NOT_IN,
  IS_EMPTY,
  IS_NOT_EMPTY,
  BEGINS_WITH;

  public static Operator getByName(String operatorName) {
    return Arrays.stream(values())
        .filter(operator -> operator.getName().equalsIgnoreCase(operatorName))
        .findAny()
        .orElse(null);
  }

  @JsonValue
  public String getName() {
    return name().toLowerCase();
  }
}
