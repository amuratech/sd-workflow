package com.kylas.sales.workflow.common.dto;

import static com.kylas.sales.workflow.common.dto.WorkflowCondition.Operator.AND;
import static com.kylas.sales.workflow.common.dto.WorkflowCondition.Operator.BETWEEN;
import static com.kylas.sales.workflow.common.dto.WorkflowCondition.Operator.IS_NOT_NULL;
import static com.kylas.sales.workflow.common.dto.WorkflowCondition.Operator.IS_NULL;
import static com.kylas.sales.workflow.common.dto.WorkflowCondition.Operator.NOT_BETWEEN;
import static com.kylas.sales.workflow.common.dto.WorkflowCondition.Operator.OR;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isAnyBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kylas.sales.workflow.domain.WorkflowSpecification;
import com.kylas.sales.workflow.domain.exception.InvalidConditionException;
import com.kylas.sales.workflow.domain.workflow.ConditionType;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

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

  public WorkflowCondition(@JsonProperty("conditionType") ConditionType conditionType) {
    this.conditionType = conditionType;
    this.expression = null;
  }

  @Getter
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ConditionExpression implements Serializable {

    private final ConditionExpression operand1;
    private final ConditionExpression operand2;
    private final Operator operator;
    private final String name;
    private final Object value;

    @JsonCreator
    public ConditionExpression(
        @JsonProperty("operand1") ConditionExpression operand1,
        @JsonProperty("operand2") ConditionExpression operand2,
        @JsonProperty("operator") String operator,
        @JsonProperty("name") String name,
        @JsonProperty("value") Object value) {
      this.operand1 = operand1;
      this.operand2 = operand2;
      this.operator = Operator.getByName(operator);
      this.name = name;
      this.value = value;
    }

    public ConditionExpression(Operator operator, String name, Object value) {
      this.operator = operator;
      this.name = name;
      this.value = value;
      operand1 = null;
      operand2 = null;
    }

    public Specification<Workflow> toSpecification() {
      switch (this.getOperator()) {
        case AND:
          return operand1.toSpecification().and(operand2.toSpecification());
        case OR:
          return operand1.toSpecification().or(operand2.toSpecification());
        case EQUAL:
          return WorkflowSpecification.hasFieldEqualsTo(name, value);
        case NOT_EQUAL:
          return WorkflowSpecification.fieldIsNotEqualsTo(name, value);
        case GREATER:
          return WorkflowSpecification.hasFieldGreaterTo(name, value);
        case GREATER_OR_EQUAL:
          return WorkflowSpecification.hasFieldGreaterOrEqualTo(name, value);
        case LESS:
          return WorkflowSpecification.hasFieldLessTo(name, value);
        case LESS_OR_EQUAL:
          return WorkflowSpecification.hasFieldLessOrEqualTo(name, value);
        case IS_NOT_NULL:
          return WorkflowSpecification.fieldIsNotNull(name);
        case IS_NULL:
          return WorkflowSpecification.fieldIsNull(name);
        case BETWEEN:
          return WorkflowSpecification.fieldIsBetween(name, value);
        case NOT_BETWEEN:
          return WorkflowSpecification.fieldIsNotBetween(name, value);
      }
      throw new InvalidConditionException();
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
      if (operator.equals(BETWEEN) || operator.equals(NOT_BETWEEN)) {
        if (!(value instanceof List)) {
          log.error("Condition operator {} value {} is not a list.", operator, value);
          throw new InvalidConditionException();
        }
        List values = (List) value;
        if (values.size() != 2) {
          log.error("Condition operator {} expects value of size 2.", operator);
          throw new InvalidConditionException();
        }
        if (isAnyBlank(String.valueOf(values.get(0)), String.valueOf(values.get(1)))) {
          log.error("Condition operator {} expects non-blank values.", operator);
          throw new InvalidConditionException();
        }
      }
      if (isBlank(String.valueOf(value))) {
        log.info("Condition job operator {} expects non-blank value.", operator);
        throw new InvalidConditionException();
      }
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
    BETWEEN,
    NOT_BETWEEN;

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
}
