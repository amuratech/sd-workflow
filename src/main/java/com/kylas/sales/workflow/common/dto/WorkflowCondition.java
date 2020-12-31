package com.kylas.sales.workflow.common.dto;

import static com.kylas.sales.workflow.common.dto.WorkflowCondition.Operator.AND;
import static com.kylas.sales.workflow.common.dto.WorkflowCondition.Operator.IS_NOT_NULL;
import static com.kylas.sales.workflow.common.dto.WorkflowCondition.Operator.IS_NULL;
import static com.kylas.sales.workflow.common.dto.WorkflowCondition.Operator.OR;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.beanutils.BeanUtils.getNestedProperty;
import static org.apache.commons.lang3.StringUtils.isAnyBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kylas.sales.workflow.domain.exception.InvalidConditionException;
import com.kylas.sales.workflow.domain.workflow.ConditionType;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Slf4j
public class WorkflowCondition {

  private final ConditionType conditionType;
  private final ConditionExpression expression;

  @JsonCreator
  public WorkflowCondition(
      @JsonProperty("conditionType") ConditionType conditionType,
      @JsonProperty("expression") ConditionExpression expression) {
    this.conditionType = conditionType;
    this.expression = expression;
  }

  @Getter
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ConditionExpression implements Serializable {

    private final ConditionExpression operand1;
    private final ConditionExpression operand2;
    private final Operator operator;
    private final String name;
    private final Object value;
    private final TriggerType triggerOn;

    @JsonCreator
    public ConditionExpression(
        @JsonProperty("operand1") ConditionExpression operand1,
        @JsonProperty("operand2") ConditionExpression operand2,
        @JsonProperty("operator") String operator,
        @JsonProperty("name") String name,
        @JsonProperty("value") Object value,
        @JsonProperty("triggerOn") String triggerOn) {
      this.operand1 = operand1;
      this.operand2 = operand2;
      this.operator = Operator.getByName(operator);
      this.name = name;
      this.value = value;
      this.triggerOn = TriggerType.valueOf(triggerOn);
    }

    public ConditionExpression(ConditionExpression operand1, ConditionExpression operand2, Operator operator, TriggerType triggerType) {
      this.operand1 = operand1;
      this.operand2 = operand2;
      this.operator = operator;
      this.name = null;
      this.value = null;
      this.triggerOn = triggerType;
    }

    public ConditionExpression(Operator operator, String name, Object value, TriggerType triggerType) {
      this.operator = operator;
      this.name = name;
      this.value = value;
      operand1 = null;
      operand2 = null;
      this.triggerOn = triggerType;
    }

    public void validate() {
      if (operator.equals(AND) || operator.equals(OR)) {
        if (isNull(operand1) || isNull(operand2)) {
          log.error("Condition operator {} expects non-null operands.", operator);
          throw new InvalidConditionException();
        }
        operand1.validate();
        operand2.validate();
        return;
      }
      if (isAnyBlank(operator.name(), name)) {
        log.error("Condition operator or key cannot be blank.");
        throw new InvalidConditionException();
      }
      if (operator.equals(IS_NULL) || operator.equals(IS_NOT_NULL)) {
        return;
      }
      if (isNull(value)) {
        log.error("Condition value can not be null.");
        throw new InvalidConditionException();
      }
      if (isBlank(String.valueOf(value))) {
        log.info("Condition job operator {} expects non-blank value.", operator);
        throw new InvalidConditionException();
      }
    }

    public boolean isSatisfiedBy(Object entity) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {

      String actualValue = nonNull(name) ? getNestedProperty(entity, name) : null;

      switch (operator) {
        case AND:
          return operand1.isSatisfiedBy(entity) && operand2.isSatisfiedBy(entity);
        case OR:
          return operand1.isSatisfiedBy(entity) || operand2.isSatisfiedBy(entity);
        case EQUAL:
          return actualValue.equals(String.valueOf(value));
        case NOT_EQUAL:
          return !actualValue.equals(String.valueOf(value));
        case IS_NOT_NULL:
          return !isNull(actualValue);
        case IS_NULL:
          return isNull(actualValue);
        case CONTAINS:
          return actualValue.contains(String.valueOf(value));
        case NOT_CONTAINS:
          return !actualValue.contains(String.valueOf(value));
        case GREATER:
          return Double.parseDouble(actualValue) > Double.parseDouble(String.valueOf(value));
        case GREATER_OR_EQUAL:
          return Double.parseDouble(actualValue) >= Double.parseDouble(String.valueOf(value));
        case LESS:
          return Double.parseDouble(actualValue) < Double.parseDouble(String.valueOf(value));
        case LESS_OR_EQUAL:
          return Double.parseDouble(actualValue) <= Double.parseDouble(String.valueOf(value));
        case IN:
          return Arrays.asList(String.valueOf(value).split("\\s*,\\s*")).contains(actualValue);
        case NOT_IN:
          return !Arrays.asList(String.valueOf(value).split("\\s*,\\s*")).contains(actualValue);
      }
      throw new InvalidConditionException();
    }
  }

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

    public String getName() {
      return name().toLowerCase();
    }
  }

  @Getter
  @AllArgsConstructor
  public enum TriggerType {
    NEW_VALUE
  }
}
