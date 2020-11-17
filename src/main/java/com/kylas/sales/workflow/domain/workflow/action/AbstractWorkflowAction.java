package com.kylas.sales.workflow.domain.workflow.action;

import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.domain.processor.Actionable;
import com.kylas.sales.workflow.domain.processor.lead.Lead;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Setter
@Getter
public abstract class AbstractWorkflowAction {

  @Id
  @GeneratedValue
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "workflow_id")
  private Workflow workflow;

  public abstract Actionable process(Lead entity);

  public abstract AbstractWorkflowAction update(ActionResponse action);

  public abstract ActionType getType();
}
