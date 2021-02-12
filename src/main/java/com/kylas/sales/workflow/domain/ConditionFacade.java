package com.kylas.sales.workflow.domain;

import static com.kylas.sales.workflow.common.dto.condition.ExpressionField.getFieldByName;
import static com.kylas.sales.workflow.common.dto.condition.Operator.AND;
import static com.kylas.sales.workflow.common.dto.condition.Operator.BETWEEN;
import static com.kylas.sales.workflow.common.dto.condition.Operator.IS_EMPTY;
import static com.kylas.sales.workflow.common.dto.condition.Operator.IS_NOT_EMPTY;
import static com.kylas.sales.workflow.common.dto.condition.Operator.IS_NOT_NULL;
import static com.kylas.sales.workflow.common.dto.condition.Operator.IS_NULL;
import static com.kylas.sales.workflow.common.dto.condition.Operator.NOT_BETWEEN;
import static com.kylas.sales.workflow.common.dto.condition.Operator.OR;
import static com.kylas.sales.workflow.domain.workflow.ConditionType.CONDITION_BASED;
import static com.kylas.sales.workflow.domain.workflow.ConditionType.FOR_ALL;
import static java.lang.Double.parseDouble;
import static java.lang.Long.parseLong;
import static java.lang.String.valueOf;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.IntStream.range;
import static org.apache.commons.beanutils.BeanUtils.getNestedProperty;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.ObjectUtils.allNotNull;
import static org.apache.commons.lang3.ObjectUtils.anyNotNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.startsWith;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylas.sales.workflow.api.request.Condition;
import com.kylas.sales.workflow.api.request.Condition.ExpressionElement;
import com.kylas.sales.workflow.common.dto.condition.Operator;
import com.kylas.sales.workflow.common.dto.condition.WorkflowCondition.ConditionExpression;
import com.kylas.sales.workflow.domain.exception.InvalidConditionException;
import com.kylas.sales.workflow.domain.processor.lead.IdName;
import com.kylas.sales.workflow.domain.service.ValueResolver;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.WorkflowCondition;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.NestedNullException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Range;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class ConditionFacade {

  private final ValueResolver valueResolver;
  private final ObjectMapper objectMapper;

  private final List<String> ID_NAME_PROPERTIES = List
      .of("pipeline", "pipelineStage", "createdBy", "updatedBy", "convertedBy", "ownerId");

  private final List<String> LIST_OF_ID_NAME_PROPERTIES = List
      .of("products");

  private final List<String> USER_FIELDS =
      List.of("createdBy", "updatedBy", "convertedBy", "ownerId");

  @Autowired
  public ConditionFacade(ValueResolver valueResolver, ObjectMapper objectMapper) {
    this.valueResolver = valueResolver;
    this.objectMapper = objectMapper;
  }

  public void validate(Condition condition) {
    if (condition.getConditionType().equals(FOR_ALL)) {
      return;
    }
    if (isEmpty(condition.getConditions())) {
      throw new InvalidConditionException();
    }
    range(0, condition.getConditions().size())
        .forEachOrdered(i -> {
          var element = condition.getConditions().get(i);
          if (isNull(element)) {
            log.info("Condition element cannot be null.");
            throw new InvalidConditionException();
          }
          if (i % 2 == 1 && !element.hasBinaryOperator()) {
            log.info("Element at odd array index cannot be AND or OR operator");
            throw new InvalidConditionException();
          }
          if (i % 2 == 0 && element.hasBinaryOperator()) {
            log.info("Element at even array index should be AND or OR operator");
            throw new InvalidConditionException();
          }
          validate(element);
        });
  }

  private void validate(ExpressionElement element) {
    Operator operator = element.getOperator();
    if ((operator.equals(AND) || operator.equals(OR))) {
      if (!anyNotNull(element.getName(), element.getValue(), element.getTriggerOn())) {
        return;
      }
      throw new InvalidConditionException();
    }
    if (!allNotNull(operator, element.getName())) {
      log.info("Expecting non-null operator and name");
      throw new InvalidConditionException();
    }
    if (operator.equals(IS_NULL) || operator.equals(IS_NOT_NULL)
        || operator.equals(IS_EMPTY) || operator.equals(IS_NOT_EMPTY)) {
      return;
    }
    if (isNull(element.getValue())) {
      log.info("Expecting non-null value for {} operator.", operator);
      throw new InvalidConditionException();
    }
    if (isBlank(String.valueOf(element.getValue()))) {
      log.info("Expecting non-blank value for {} operator.", operator);
      throw new InvalidConditionException();
    }
    if (operator.equals(BETWEEN) || operator.equals(NOT_BETWEEN)) {
      List range = valueResolver.getListFrom(valueOf(element.getValue()));
      if (range.size() != 2) {
        log.info("Condition operator {} expects two values.", operator);
        throw new InvalidConditionException();
      }
    }
    if (ID_NAME_PROPERTIES.contains(element.getName()) && !isIdNamePresent(element.getValue())) {
      log.info("IdName for {} can not be resolved", element.getName());
      throw new InvalidConditionException();
    }
  }

  private boolean isIdNamePresent(Object value) {
    IdName idName = valueResolver.getIdNameFrom(value);
    return !isNull(idName) && (!isNull(idName.getId()) && !isNull(idName.getName()));
  }

  public WorkflowCondition createFrom(Condition condition) {
    return condition.getConditionType().equals(FOR_ALL)
        ? new WorkflowCondition(FOR_ALL, null)
        : new WorkflowCondition(condition.getConditionType(), buildExpression(condition.getConditions()));
  }

  public boolean satisfies(ConditionExpression expression, Object entity) {

    if (isNotBlank(expression.getName()) && LIST_OF_ID_NAME_PROPERTIES.contains(expression.getName())) {
      List<String> actualValues = getActualValueOfList(expression, entity);
      return idNameSatisfiedForList(expression, actualValues);
    }
    String actualValue = getActualValue(expression, entity);
    if (isNotBlank(expression.getName()) && ID_NAME_PROPERTIES.contains(expression.getName())) {
      return idNameSatisfiedBy(expression, actualValue);
    }

    switch (expression.getOperator()) {
      case AND:
        return satisfies(expression.getOperand1(), entity) && satisfies(expression.getOperand2(), entity);
      case OR:
        return satisfies(expression.getOperand1(), entity) || satisfies(expression.getOperand2(), entity);
      case EQUAL:
        if (isNull(actualValue)) {
          return false;
        }
        return expression.getValue() instanceof Number
            ? Double.parseDouble(actualValue) == Double.parseDouble(valueOf(expression.getValue()))
            : valueOf(expression.getValue()).equalsIgnoreCase(actualValue);
      case NOT_EQUAL:
        if (isNull(actualValue)) {
          return true;
        }
        return expression.getValue() instanceof Number
            ? Double.parseDouble(actualValue) != Double.parseDouble(valueOf(expression.getValue()))
            : !valueOf(expression.getValue()).equalsIgnoreCase(actualValue);
      case IS_NOT_NULL:
        return !isNull(actualValue);
      case IS_NULL:
        return isNull(actualValue);
      case CONTAINS:
        return !isNull(actualValue) && actualValue.contains(valueOf(expression.getValue()));
      case NOT_CONTAINS:
        return isNull(actualValue) || !actualValue.contains(valueOf(expression.getValue()));
      case BETWEEN:
        if (isNull(actualValue)) {
          return false;
        }
        var betweenValues = valueResolver.getListFrom(valueOf(expression.getValue()));
        return Range
            .between(parseDouble(valueOf(betweenValues.get(0))), parseDouble(valueOf(betweenValues.get(1))))
            .contains(parseDouble(actualValue));
      case NOT_BETWEEN:
        if (isNull(actualValue)) {
          return true;
        }
        var notBetweenValues = valueResolver.getListFrom(valueOf(expression.getValue()));
        return !Range
            .between(parseDouble(valueOf(notBetweenValues.get(0))), parseDouble(valueOf(notBetweenValues.get(1))))
            .contains(parseDouble(actualValue));
      case GREATER:
        return !isNull(actualValue) && parseDouble(actualValue) > parseDouble(valueOf(expression.getValue()));
      case GREATER_OR_EQUAL:
        return !isNull(actualValue) && parseDouble(actualValue) >= parseDouble(valueOf(expression.getValue()));
      case LESS:
        return !isNull(actualValue) && parseDouble(actualValue) < parseDouble(valueOf(expression.getValue()));
      case LESS_OR_EQUAL:
        return !isNull(actualValue) && parseDouble(actualValue) <= parseDouble(valueOf(expression.getValue()));
      case IN:
        return Arrays.asList(valueOf(expression.getValue()).split("\\s*,\\s*")).contains(actualValue);
      case NOT_IN:
        return !Arrays.asList(valueOf(expression.getValue()).split("\\s*,\\s*")).contains(actualValue);
      case IS_EMPTY:
        return isEmpty(actualValue);
      case IS_NOT_EMPTY:
        return !isEmpty(actualValue);
      case BEGINS_WITH:
        return startsWith(actualValue, valueOf(expression.getValue()));
    }
    throw new InvalidConditionException();
  }

  private boolean idNameSatisfiedBy(ConditionExpression expression, Object actualValue) {
    switch (expression.getOperator()) {
      case EQUAL:
        return !isNull(actualValue) && (parseLong(valueOf(actualValue)) == valueResolver.getIdNameFrom(expression.getValue()).getId());
      case NOT_EQUAL:
        return isNull(actualValue) || (parseLong(valueOf(actualValue)) != valueResolver.getIdNameFrom(expression.getValue()).getId());
      case IS_NOT_NULL:
        return !isNull(actualValue);
      case IS_NULL:
        return isNull(actualValue);
    }
    throw new InvalidConditionException();
  }

  private boolean idNameSatisfiedForList(ConditionExpression expression, List<String> actualValues) {
    IdName expressionValue = new ObjectMapper().convertValue(expression.getValue(), IdName.class);
    switch (expression.getOperator()) {
      case CONTAINS:
        return actualValues.contains(expressionValue.getId().toString());
      case NOT_CONTAINS:
        return !actualValues.contains(expressionValue.getId().toString());
      case IS_EMPTY:
        return CollectionUtils.isEmpty(actualValues);
      case IS_NOT_EMPTY:
        return CollectionUtils.isNotEmpty(actualValues);
    }
    throw new InvalidConditionException();
  }

  private String getActualValue(ConditionExpression expression, Object entity) {
    String actualValue = null;
    if (nonNull(expression.getName())) {
      try {
        actualValue = getNestedProperty(entity, getFieldByName(expression.getName()));
      } catch (NestedNullException ignored) {
      } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
        log.error("Exception occurred while getting actual value for {}", expression.getName());
      }
    }
    return actualValue;
  }

  private List<String> getActualValueOfList(ConditionExpression expression, Object entity) {
    List<String> values = new ArrayList<>();
    if (nonNull(expression.getName())) {
        EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();
        ExpressionParser parser = new SpelExpressionParser();

        Optional<Object> value = Optional.ofNullable(parser.parseExpression(expression.getName()).getValue(context,entity));
        value.ifPresent(list -> ((List)list).stream().forEach(o -> {
          try {
            values.add(getNestedProperty(o, getFieldByName(expression.getName())));
          } catch (Exception e) {
            log.error("Exception occurred while getting actual value for {}", expression.getName());
          }
        }));
    }
    return values;
  }

  public Mono<ConditionExpression> nameResolved(ConditionExpression expression, String authenticationToken) {
    if (expression.getOperator().equals(AND) || expression.getOperator().equals(OR)) {
      var resolvedOperand1 = nameResolved(expression.getOperand1(), authenticationToken);
      var resolvedOperand2 = nameResolved(expression.getOperand2(), authenticationToken);
      return Mono
          .zip(resolvedOperand1, resolvedOperand2)
          .map(objects -> new ConditionExpression(
              objects.getT1(),
              objects.getT2(),
              expression.getOperator().getName(),
              expression.getName(),
              expression.getValue(),
              isNull(expression.getTriggerOn()) ? null : expression.getTriggerOn().name()));
    }

    if (ID_NAME_PROPERTIES.contains(expression.getName())) {
      var idNameMono =
          USER_FIELDS.contains(expression.getName()) ? valueResolver.getUser(expression.getValue(), authenticationToken)
              : expression.getName().equals("pipeline") ? valueResolver.getPipeline(expression.getValue(), authenticationToken)
                  : expression.getName().equals("pipelineStage") ? valueResolver.getPipelineStage(expression.getValue(), authenticationToken)
                      : valueResolver.getProduct(expression.getValue(), authenticationToken);
      return idNameMono
          .map(idName -> getExpressionWithValue(expression, idName))
          .defaultIfEmpty(getExpressionWithValue(expression, null));
    }
    return Mono.just(expression);
  }

  private ConditionExpression getExpressionWithValue(ConditionExpression expression, IdName value) {
    return new ConditionExpression(
        expression.getOperand1(),
        expression.getOperand2(),
        expression.getOperator().getName(),
        expression.getName(),
        value,
        expression.getTriggerOn().name());
  }

  public ConditionExpression buildExpression(List<ExpressionElement> expressionElements) {
    var elements = expressionElements.stream()
        .map(this::conditionExpressionFrom)
        .collect(Collectors.toList());

    var andIndex = indexOfTerminalOperator(AND, elements);
    while (andIndex.isPresent()) {
      var sublist = elements.subList(andIndex.getAsInt() - 1, andIndex.getAsInt() + 2);
      var subExpression = new ConditionExpression(sublist.get(0), sublist.get(2), AND);
      sublist.clear();
      elements.add(andIndex.getAsInt() - 1, subExpression);
      andIndex = indexOfTerminalOperator(AND, elements);
    }

    var orIndex = indexOfTerminalOperator(OR, elements);
    while (orIndex.isPresent()) {
      var sublist = elements.subList(orIndex.getAsInt() - 1, orIndex.getAsInt() + 2);
      var subExpression = new ConditionExpression(sublist.get(0), sublist.get(2), OR);
      sublist.clear();
      elements.add(orIndex.getAsInt() - 1, subExpression);
      orIndex = indexOfTerminalOperator(OR, elements);
    }
    return elements.get(0);
  }

  /*This should be inorder traversal*/
  public List<ExpressionElement> flattenExpression(ConditionExpression expression) {
    return inOrderTraverse(expression, new ArrayList<>());
  }

  private List<ExpressionElement> inOrderTraverse(ConditionExpression expression, List<ExpressionElement> elements) {
    if (isNull(expression)) {
      return elements;
    }
    inOrderTraverse(expression.getOperand1(), elements);
    elements.add(conditionElementFrom(expression));
    inOrderTraverse(expression.getOperand2(), elements);
    return elements;
  }

  private ExpressionElement conditionElementFrom(ConditionExpression expression) {
    if (expression.getOperator().equals(AND) || expression.getOperator().equals(OR)) {
      return new ExpressionElement(expression.getOperator().name(), null, null, null);
    }
    return new ExpressionElement(
        expression.getOperator().name(),
        expression.getName(),
        expression.getValue(),
        expression.getTriggerOn().name());
  }

  private OptionalInt indexOfTerminalOperator(Operator operator, List<ConditionExpression> elements) {
    return range(0, elements.size())
        .filter(i -> elements.get(i).isTerminalOperator(operator))
        .findFirst();
  }

  private ConditionExpression conditionExpressionFrom(ExpressionElement element) {
    return (element.getOperator().equals(AND) || element.getOperator().equals(OR))
        ? new ConditionExpression(null, null, element.getOperator())
        : new ConditionExpression(element.getOperator(), element.getName(), element.getValue(), element.getTriggerOn());
  }

  public WorkflowCondition update(@NotNull Condition requestedCondition, Workflow workflow) {
    var expression = requestedCondition.getConditionType().equals(CONDITION_BASED)
        ? buildExpression(requestedCondition.getConditions())
        : null;
    return workflow.getWorkflowCondition().update(requestedCondition.getConditionType(), expression);
  }
}
