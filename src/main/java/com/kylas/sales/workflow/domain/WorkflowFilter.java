package com.kylas.sales.workflow.domain;

import com.kylas.sales.workflow.domain.exception.InvalidFilterException;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

@Slf4j
public class WorkflowFilter {
  private final String operator;
  private final String fieldName;
  private final String fieldType;
  private final Object value;

  public WorkflowFilter(String operator, String fieldName, String fieldType, Object value) {
    verify(operator, fieldName, fieldType, value);
    this.operator = operator;
    this.fieldName = fieldName;
    this.fieldType = fieldType;
    this.value = convertValue(fieldName, value);
  }

  private Object convertValue(String fieldName, Object value) {
    try {
      if ("entityType".equalsIgnoreCase(fieldName) && value != null) {
        return EntityType.valueOf(value.toString().toUpperCase());
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new InvalidFilterException();
    }
    return value;
  }

  private void verify(String operator, String fieldName, String fieldType, Object fieldValue) {

    if (StringUtils.isBlank(operator)
        || StringUtils.isBlank(fieldName)
        || StringUtils.isBlank(fieldType)) {
      log.info(
          "Search job operator {}, fieldName {}, fieldType {} can not be blank",
          operator,
          fieldName,
          fieldType);
      throw new InvalidFilterException();
    }
    if ("is_null".equalsIgnoreCase(operator) || "is_not_null".equalsIgnoreCase(operator)) {
      return;
    }
    if (fieldValue == null) {
      log.info("Search job operator {} value can not be blank", operator);
      throw new InvalidFilterException();
    }
    if ("between".equalsIgnoreCase(operator) || "not_between".equalsIgnoreCase(operator)) {
      if (!(fieldValue instanceof List)) {
        log.info(
            "Search job operator {} and value {}  is mismatch value must be of list",
            operator,
            fieldValue);
        throw new InvalidFilterException();
      }
      List values = (List) fieldValue;
      if (values.size() != 2) {
        log.info(
            "Search job operator {} and value {}  is mismatch size is 2 expected",
            operator,
            fieldValue);
        throw new InvalidFilterException();
      }
      if (StringUtils.isBlank(String.valueOf(values.get(0)))
          || StringUtils.isBlank(String.valueOf(values.get(1)))) {
        log.info(
            "Search job operator {} and value {}  both value must not be blank",
            operator,
            fieldValue);
        throw new InvalidFilterException();
      }
    }
    if ("".equals(fieldValue)) {
      log.info("Search job operator {} value must not be blank", operator, fieldValue);
      throw new InvalidFilterException();
    }
  }

  public Specification<Workflow> toSpecification() {
    switch (this.operator) {
      case "equal":
        return WorkflowSpecification.hasFieldEqualsTo(this.fieldName, this.value);
      case "not_equal":
        return WorkflowSpecification.fieldIsNotEqualsTo(this.fieldName, this.value);
      case "greater":
        return WorkflowSpecification.hasFieldGreaterTo(this.fieldName, this.value);
      case "greater_or_equal":
        return WorkflowSpecification.hasFieldGreaterOrEqualTo(this.fieldName, this.value);
      case "less":
        return WorkflowSpecification.hasFieldLessTo(this.fieldName, this.value);
      case "less_or_equal":
        return WorkflowSpecification.hasFieldLessOrEqualTo(this.fieldName, this.value);
      case "is_not_null":
        return WorkflowSpecification.fieldIsNotNull(this.fieldName);
      case "is_null":
        return WorkflowSpecification.fieldIsNull(this.fieldName);
      case "between":
        return WorkflowSpecification.fieldIsBetween(this.fieldName, this.value);
      case "not_between":
        return WorkflowSpecification.fieldIsNotBetween(this.fieldName, this.value);
    }
    throw new InvalidFilterException();
  }
}
