package com.kylas.sales.workflow.domain.workflow;

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

@Entity
@Getter
@Setter
@NoArgsConstructor
public class WorkflowCondition {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Enumerated(value = EnumType.STRING)
  private ConditionType type;

  @OneToOne
  @JoinColumn(name = "workflow_id")
  private Workflow workflow;

  private WorkflowCondition(ConditionType type) {
    this.type = type;
  }

  public static WorkflowCondition createNew(com.kylas.sales.workflow.common.dto.WorkflowCondition condition) {
    return new WorkflowCondition(condition.getConditionType());
  }
}
