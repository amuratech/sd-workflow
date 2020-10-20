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
import lombok.ToString;

@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString
public class WorkflowTrigger {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Enumerated(value = EnumType.STRING)
  private TriggerType triggerType;
  @Enumerated(value = EnumType.STRING)
  private TriggerFrequency triggerFrequency;

  @OneToOne
  @JoinColumn(name = "workflow_id")
  private Workflow workflow;
  private WorkflowTrigger(TriggerType triggerType, TriggerFrequency triggerFrequency) {
    this.triggerType = triggerType;
    this.triggerFrequency = triggerFrequency;
  }

  public static WorkflowTrigger createNew(com.kylas.sales.workflow.common.dto.WorkflowTrigger trigger) {
    return new WorkflowTrigger(trigger.getName(),trigger.getTriggerFrequency());
  }
}