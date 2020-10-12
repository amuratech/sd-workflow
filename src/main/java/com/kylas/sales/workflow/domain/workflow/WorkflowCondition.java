package com.kylas.sales.workflow.domain.workflow;

import com.kylas.sales.workflow.api.request.ConditionRequest;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
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

  private WorkflowCondition(ConditionType type) {
    this.type = type;
  }

  public static WorkflowCondition createNew(ConditionRequest condition) {
    return new WorkflowCondition(condition.getConditionType());
  }
}
