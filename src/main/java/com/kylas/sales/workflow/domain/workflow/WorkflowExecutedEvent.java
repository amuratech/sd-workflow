package com.kylas.sales.workflow.domain.workflow;

import java.util.Date;
import javax.persistence.Entity;
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
public class WorkflowExecutedEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Date lastTriggeredAt;
  private long triggerCount;

  @OneToOne
  @JoinColumn(name = "workflow_id")
  private Workflow workflow;

  private WorkflowExecutedEvent(Date lastTriggeredAt, long triggerCount, Workflow workflow) {
    this.lastTriggeredAt = lastTriggeredAt;
    this.triggerCount = triggerCount;
    this.workflow = workflow;
  }

  public static WorkflowExecutedEvent createNew(Workflow workflow) {
    return new WorkflowExecutedEvent(null, 0, workflow);
  }
}
