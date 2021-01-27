package com.kylas.sales.workflow.domain.workflow;

import com.kylas.sales.workflow.common.dto.condition.WorkflowCondition.ConditionExpression;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
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

  public WorkflowCondition(ConditionType type, ConditionExpression expression) {
    this.type = type;
    this.expression = expression;
  }

  private WorkflowCondition(Long id, ConditionType type, ConditionExpression expression, Workflow workflow) {
    this.id = id;
    this.type = type;
    this.expression = expression;
    this.workflow = workflow;
  }

  public WorkflowCondition update(ConditionType type, ConditionExpression expression) {
    return new WorkflowCondition(this.id, type, expression, this.workflow);
  }

}
