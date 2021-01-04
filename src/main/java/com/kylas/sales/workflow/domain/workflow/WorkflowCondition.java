package com.kylas.sales.workflow.domain.workflow;

import static com.kylas.sales.workflow.domain.workflow.ConditionType.CONDITION_BASED;
import static com.kylas.sales.workflow.domain.workflow.ConditionType.FOR_ALL;
import static java.util.Objects.isNull;

import com.kylas.sales.workflow.common.dto.condition.WorkflowCondition.ConditionExpression;
import com.kylas.sales.workflow.domain.exception.InvalidConditionException;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.lang.reflect.InvocationTargetException;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Entity
@Getter
@Setter
@NoArgsConstructor
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Slf4j
public class WorkflowCondition {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(value = EnumType.STRING)
  private ConditionType type;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private ConditionExpression expression;

  @OneToOne
  @JoinColumn(name = "workflow_id")
  private Workflow workflow;

  private WorkflowCondition(ConditionType type, ConditionExpression expression) {
    this.type = type;
    this.expression = expression;
  }

  private WorkflowCondition(Long id, ConditionType type, ConditionExpression expression, Workflow workflow) {
    this.id = id;
    this.type = type;
    this.expression = expression;
    this.workflow = workflow;
  }

  public static WorkflowCondition createNew(com.kylas.sales.workflow.common.dto.condition.WorkflowCondition condition) {
    validate(condition);
    return new WorkflowCondition(condition.getConditionType(), condition.getExpression());
  }

  private static void validate(com.kylas.sales.workflow.common.dto.condition.WorkflowCondition condition) {
    if (condition.getConditionType().equals(CONDITION_BASED)) {
      if (isNull(condition.getExpression())) {
        throw new InvalidConditionException();
      }
      condition.getExpression().validate();
    }
  }

  public WorkflowCondition update(com.kylas.sales.workflow.common.dto.condition.WorkflowCondition condition) {
    validate(condition);
    return new WorkflowCondition(this.id, condition.getConditionType(), condition.getExpression(), this.workflow);
  }

  public boolean isSatisfiedBy(Object entity) {
    if (type.equals(FOR_ALL)) {
      return true;
    }
    try {
      return expression.isSatisfiedBy(entity);
    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      log.error("Exception while resolving expression {} from entity.", expression.toString());
      throw new InvalidConditionException();
    }
  }
}
