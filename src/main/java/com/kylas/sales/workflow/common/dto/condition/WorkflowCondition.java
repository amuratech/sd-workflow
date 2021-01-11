package com.kylas.sales.workflow.common.dto.condition;

import static com.kylas.sales.workflow.common.dto.condition.ExpressionField.getFieldByName;
import static com.kylas.sales.workflow.common.dto.condition.Operator.AND;
import static com.kylas.sales.workflow.common.dto.condition.Operator.BETWEEN;
import static com.kylas.sales.workflow.common.dto.condition.Operator.IS_NOT_NULL;
import static com.kylas.sales.workflow.common.dto.condition.Operator.IS_NULL;
import static com.kylas.sales.workflow.common.dto.condition.Operator.NOT_BETWEEN;
import static com.kylas.sales.workflow.common.dto.condition.Operator.OR;
import static java.lang.Double.parseDouble;
import static java.lang.String.valueOf;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.beanutils.BeanUtils.getNestedProperty;
import static org.apache.commons.lang3.StringUtils.isAnyBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kylas.sales.workflow.domain.exception.InvalidConditionException;
import com.kylas.sales.workflow.domain.processor.lead.IdName;
import com.kylas.sales.workflow.domain.service.ValueResolver;
import com.kylas.sales.workflow.domain.workflow.ConditionType;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.NestedNullException;
import org.apache.commons.lang3.Range;
import reactor.core.publisher.Mono;

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

    @Getter(AccessLevel.NONE)
    private final List<String> ID_NAME_PROPERTIES = List.of("pipeline", "pipelineStage", "products");
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
      this.triggerOn = (this.operator.equals(AND) || this.operator.equals(OR))? null: TriggerType.valueOf(triggerOn);
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
      if (isBlank(valueOf(value))) {
        log.info("Condition operator {} expects non-blank value.", operator);
        throw new InvalidConditionException();
      }
      if (operator.equals(BETWEEN) || operator.equals(NOT_BETWEEN)) {
        List range = ValueResolver.getListFrom(valueOf(value));
        if (range.size() != 2) {
          log.info("Condition operator {} expects two values.", operator);
          throw new InvalidConditionException();
        }
      }
      if (ID_NAME_PROPERTIES.contains(name)) {
        ValueResolver.getIdNameFrom(value);
      }
    }

    public boolean isSatisfiedBy(Object entity) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {

      String actualValue = getActualValue(entity);
      if (isNotBlank(name) && ID_NAME_PROPERTIES.contains(name)) {
        return idNameSatisfiedBy(actualValue);
      }

      switch (operator) {
        case AND:
          return operand1.isSatisfiedBy(entity) && operand2.isSatisfiedBy(entity);
        case OR:
          return operand1.isSatisfiedBy(entity) || operand2.isSatisfiedBy(entity);
        case EQUAL:
          return value instanceof Number
              ? Double.parseDouble(actualValue) == Double.parseDouble(valueOf(value))
              : valueOf(value).equalsIgnoreCase(actualValue);
        case NOT_EQUAL:
          return value instanceof Number
              ? Double.parseDouble(actualValue) != Double.parseDouble(valueOf(value))
              : !valueOf(value).equalsIgnoreCase(actualValue);
        case IS_NOT_NULL:
          return !isNull(actualValue);
        case IS_NULL:
          return isNull(actualValue);
        case CONTAINS:
          return !isNull(actualValue) && actualValue.contains(valueOf(value));
        case NOT_CONTAINS:
          return isNull(actualValue) && !actualValue.contains(valueOf(value));
        case BETWEEN:
          var betweenValues = ValueResolver.getListFrom(valueOf(value));
          return Range
              .between(parseDouble(valueOf(betweenValues.get(0))), parseDouble(valueOf(betweenValues.get(1))))
              .contains(parseDouble(actualValue));
        case NOT_BETWEEN:
          var notBetweenValues = ValueResolver.getListFrom(valueOf(value));
          return !Range
              .between(parseDouble(valueOf(notBetweenValues.get(0))), parseDouble(valueOf(notBetweenValues.get(1))))
              .contains(parseDouble(actualValue));
        case GREATER:
          return parseDouble(actualValue) > parseDouble(valueOf(value));
        case GREATER_OR_EQUAL:
          return parseDouble(actualValue) >= parseDouble(valueOf(value));
        case LESS:
          return parseDouble(actualValue) < parseDouble(valueOf(value));
        case LESS_OR_EQUAL:
          return parseDouble(actualValue) <= parseDouble(valueOf(value));
        case IN:
          return Arrays.asList(valueOf(value).split("\\s*,\\s*")).contains(actualValue);
        case NOT_IN:
          return !Arrays.asList(valueOf(value).split("\\s*,\\s*")).contains(actualValue);
      }
      throw new InvalidConditionException();
    }

    private boolean idNameSatisfiedBy(Object actualValue) {
      if (isNull(value)) {
        return false;
      }
      IdName conditionValue = ValueResolver.getIdNameFrom(value);
      switch (operator) {
        case EQUAL:
          return !isNull(actualValue) && Long.parseLong(valueOf(actualValue)) == conditionValue.getId();
        case NOT_EQUAL:
          return !isNull(actualValue) && Long.parseLong(valueOf(actualValue)) != conditionValue.getId();
        case IS_NOT_NULL:
          return !isNull(actualValue);
        case IS_NULL:
          return isNull(actualValue);
      }
      throw new InvalidConditionException();
    }

    private String getActualValue(Object entity) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
      String actualValue = null;
      if (nonNull(name)) {
        try {
          actualValue = getNestedProperty(entity, getFieldByName(name));
        } catch (NestedNullException ignored) {
        }
      }
      return actualValue;
    }

    public Mono<ConditionExpression> nameResolved(String authenticationToken) {
      if (operator.equals(AND) || operator.equals(OR)) {
        var resolvedOperand1 = this.operand1.nameResolved(authenticationToken);
        var resolvedOperand2 = this.operand2.nameResolved(authenticationToken);
        return Mono
            .zip(resolvedOperand1, resolvedOperand2)
            .map(objects -> new ConditionExpression(objects.getT1(), objects.getT2(), operator.getName(), name, value, triggerOn.name()));
      }

      if (ID_NAME_PROPERTIES.contains(name)) {
        var idNameMono = name.equals("pipeline") ? ValueResolver.getPipeline(value, authenticationToken)
            : name.equals("pipelineStage") ? ValueResolver.getPipelineStage(value, authenticationToken)
                : ValueResolver.getProduct(value, authenticationToken);
        return idNameMono
            .map(idName -> new ConditionExpression(operand1, operand2, operator.getName(), name, idName, triggerOn.name()));
      }
      return Mono.just(this);
    }
  }

  @Getter
  @AllArgsConstructor
  public enum TriggerType {
    NEW_VALUE
  }
}
