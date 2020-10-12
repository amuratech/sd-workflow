package com.kylas.sales.workflow.domain.workflow;

import com.kylas.sales.workflow.domain.exception.InsufficientPrivilegeException;
import com.kylas.sales.workflow.domain.exception.InvalidWorkflowPropertyException;
import com.kylas.sales.workflow.domain.user.User;
import com.kylas.sales.workflow.domain.workflow.action.AbstractWorkflowAction;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class Workflow {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @NotBlank
  @Size(min = 3, max = 255)
  private String name;
  private String description;
  @Enumerated(value = EnumType.STRING)
  private EntityType entityType;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "workflow_trigger_id")
  private WorkflowTrigger workflowTrigger;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "workflow_condition_id")
  private WorkflowCondition workflowCondition;


  @OneToMany(cascade = CascadeType.ALL, mappedBy = "workflow", targetEntity = AbstractWorkflowAction.class)
  private Set<WorkflowAction> workflowActions;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "created_by")
  private User createdBy;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "updated_by")
  private User updatedBy;

  private Long tenantId;
  private Date createdAt;
  private Date updatedAt;

  private Workflow(String name, String description, EntityType entityType, WorkflowTrigger workflowTrigger,
      Set<WorkflowAction> workflowActions, WorkflowCondition condition, long tenantId,
      User createdBy,
      User updatedBy) {
    var now = new Date();
    this.name = name;
    this.description = description;
    this.entityType = entityType;
    this.workflowTrigger = workflowTrigger;
    this.tenantId = tenantId;
    this.createdBy = createdBy;
    this.updatedBy = updatedBy;
    this.createdAt = now;
    this.updatedAt = now;
    this.workflowActions = workflowActions.stream().map(workflowAction -> {
      workflowAction.setWorkflow(this);
      return workflowAction;
    }).collect(Collectors.toSet());
    this.workflowCondition = condition;
  }

  public static Workflow createNew(String name, String description, EntityType entityType,
      WorkflowTrigger trigger, User creator, Set<WorkflowAction> workflowActions,
      WorkflowCondition condition) {
    if (!creator.canCreateWorkflow()) {
      log.error("User with id: {} does not have create permission on Workflow", creator.getId());
      throw new InsufficientPrivilegeException();
    }
    if(entityType == null){
      log.error("Try to create workflow with entityType {}",entityType);
      throw new InvalidWorkflowPropertyException();
    }
    if(trigger == null){
      log.error("Try to create workflow with trigger {}",trigger);
      throw new InvalidWorkflowPropertyException();
    }
    if(workflowActions == null){
      log.error("Try to create workflow with action {}",workflowActions);
      throw new InvalidWorkflowPropertyException();
    }
    if(condition == null){
      log.error("Try to create workflow with condition {}",condition);
      throw new InvalidWorkflowPropertyException();
    }
    return new Workflow(name, description, entityType, trigger, workflowActions, condition, creator.getTenantId(), creator, creator);
  }
}
