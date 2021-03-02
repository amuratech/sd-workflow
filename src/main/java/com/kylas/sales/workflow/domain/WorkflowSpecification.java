package com.kylas.sales.workflow.domain;

import com.kylas.sales.workflow.domain.exception.InvalidFilterException;
import com.kylas.sales.workflow.domain.user.User_;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.TriggerFrequency;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.WorkflowTrigger_;
import com.kylas.sales.workflow.domain.workflow.Workflow_;
import com.kylas.sales.workflow.integration.IntegrationConfig;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

@Slf4j
public class WorkflowSpecification {

  static Specification<Workflow> belongToTenant(long tenantId) {
    return (root, criteria, builder) -> builder.equal(root.get(Workflow_.tenantId), tenantId);
  }

  static Specification<Workflow> belongToUser(long userId) {
    return (root, criteria, builder) -> builder.equal(root.get(Workflow_.createdBy).get(User_.id), userId);
  }

  static Specification<Workflow> withId(long workflowId) {
    return (root, criteria, builder) -> builder.equal(root.get(Workflow_.id), workflowId);
  }

  static Specification<Workflow> withEntityType(EntityType entityType) {
    return (root, criteria, builder) -> builder.equal(root.get(Workflow_.entityType), entityType);
  }

  static Specification<Workflow> withTriggerFrequency(TriggerFrequency triggerFrequency) {
    return (root, criteria, builder) -> builder.equal(root.get(Workflow_.workflowTrigger).get(WorkflowTrigger_.triggerFrequency), triggerFrequency);
  }

  static Specification<Workflow> active() {
    return (root, criteria, builder) -> builder.equal(root.get(Workflow_.active), true);
  }

  static Specification<Workflow> systemDefault() {
    return (root, criteria, builder) -> builder.equal(root.get(Workflow_.systemDefault), true);
  }

  public static Specification<Workflow> hasFieldEqualsTo(String fieldName, Object value) {
    return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get(fieldName), value);
  }

  public static Specification<Workflow> fieldIsNotEqualsTo(String fieldName, Object value) {
    return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get(fieldName), value).not();
  }

  public static Specification<Workflow> hasFieldGreaterTo(String fieldName, Object value) {
    return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.greaterThan(root.get(fieldName),convertToOffsetDateTime(value.toString()));
  }
  public static Specification<Workflow> hasFieldGreaterOrEqualTo(String fieldName, Object value) {
    return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get(fieldName),convertToOffsetDateTime(value.toString()));
  }

  public static Specification<Workflow> hasFieldLessTo(String fieldName, Object value) {
    return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.lessThan(root.get(fieldName),convertToOffsetDateTime(value.toString()));
  }

  public static Specification<Workflow> hasFieldLessOrEqualTo(String fieldName, Object value) {
    return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get(fieldName),convertToOffsetDateTime(value.toString()));
  }

  public static Specification<Workflow> fieldIsNotNull(String fieldName) {
    return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.isNotNull(root.get(fieldName));
  }

  public static Specification<Workflow> fieldIsNull(String fieldName) {
    return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.isNull(root.get(fieldName));
  }

  public static Specification<Workflow> fieldIsBetween(String fieldName,Object value) {
    List<String> values = (List<String>) value;
    return (root, criteriaQuery, criteriaBuilder) ->
        criteriaBuilder.between(root.get(fieldName), convertToOffsetDateTime(values.get(0)), convertToOffsetDateTime(values.get(1)));
  }

  public static Specification<Workflow> fieldIsNotBetween(String fieldName, Object value) {
    List<String> values = (List<String>) value;
    return (root, criteriaQuery, criteriaBuilder) ->
        criteriaBuilder.between(root.get(fieldName), convertToOffsetDateTime(values.get(0)), convertToOffsetDateTime(values.get(1))).not();
  }

  private static Date convertToOffsetDateTime(String value) {
    try {
      OffsetDateTime parse = OffsetDateTime.parse(value);
      Date from = Date.from(parse.toInstant());
      return from;
    } catch (DateTimeParseException ex) {
      log.error("Unable to parse date " + value, ex);
      throw new InvalidFilterException();
    }
  }

  public static Specification<Workflow> systemDefaultConfiguration(IntegrationConfig config) {
    return systemDefault()
        .and(withEntityType(config.getEntityType())
            .and(withTriggerFrequency(config.getTrigger().getTriggerFrequency())));
  }
}
