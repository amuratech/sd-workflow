package com.kylas.sales.workflow.common.dto.condition;

import com.fasterxml.jackson.annotation.JsonValue;
import com.kylas.sales.workflow.domain.exception.InvalidConditionException;
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
  IN,
  NOT_IN;

  public static Operator getByName(String operatorName) {
    return Arrays.stream(values())
        .filter(operator -> operator.getName().equalsIgnoreCase(operatorName))
        .findAny()
        .orElseThrow(InvalidConditionException::new);
  }

  @JsonValue
  public String getName() {
    return name().toLowerCase();
  }
}
