package com.kylas.sales.workflow.domain.workflow;

import static org.springframework.util.CollectionUtils.isEmpty;

import com.kylas.sales.workflow.domain.exception.InsufficientPrivilegeException;
import com.kylas.sales.workflow.domain.exception.InvalidWorkflowPropertyException;
import com.kylas.sales.workflow.domain.user.Action;
import com.kylas.sales.workflow.domain.user.User;
import com.kylas.sales.workflow.domain.workflow.action.AbstractWorkflowAction;
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
import javax.persistence.Transient;
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

  @OneToOne(mappedBy = "workflow", cascade = CascadeType.ALL)
  private WorkflowTrigger workflowTrigger;

  @OneToOne(mappedBy = "workflow", cascade = CascadeType.ALL)
  private WorkflowCondition workflowCondition;

  @OneToOne(mappedBy = "workflow", cascade = CascadeType.ALL)
  private WorkflowExecutedEvent workflowExecutedEvent;

  @OneToMany(
      fetch = FetchType.EAGER,
      cascade = CascadeType.ALL,
      mappedBy = "workflow",
      orphanRemoval = true,
      targetEntity = AbstractWorkflowAction.class)
  private Set<AbstractWorkflowAction> workflowActions;

  private boolean active;

  private boolean systemDefault;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "created_by")
  private User createdBy;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "updated_by")
  private User updatedBy;

  private Long tenantId;
  private Date createdAt;
  private Date updatedAt;

  @Transient
  private Action allowedActions;

  private Workflow(Long id, @NotBlank @Size(min = 3, max = 255) String name, String description,
      EntityType entityType, WorkflowTrigger workflowTrigger, WorkflowCondition workflowCondition,
      Set<AbstractWorkflowAction> workflowActions, boolean active, User createdBy, User updatedBy, Long tenantId, Date createdAt,
      Date updatedAt, Action allowedActions, WorkflowExecutedEvent executedEvent, boolean systemDefault) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.entityType = entityType;
    this.workflowTrigger = workflowTrigger;
    workflowTrigger.setWorkflow(this);
    this.workflowCondition = workflowCondition;
    workflowCondition.setWorkflow(this);
    this.workflowActions = workflowActions;
    workflowActions.forEach(workflowAction -> workflowAction.setWorkflow(this));
    this.active = active;
    this.createdBy = createdBy;
    this.updatedBy = updatedBy;
    this.tenantId = tenantId;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.allowedActions = allowedActions;
    this.workflowExecutedEvent = executedEvent;
    this.systemDefault = systemDefault;
  }

  private Workflow(String name, String description, EntityType entityType, WorkflowTrigger workflowTrigger,
      Set<AbstractWorkflowAction> workflowActions, WorkflowCondition condition, long tenantId,
      User createdBy, User updatedBy, boolean active) {
    var now = new Date();
    this.name = name;
    this.description = description;
    this.entityType = entityType;
    workflowTrigger.setWorkflow(this);
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
    condition.setWorkflow(this);
    this.workflowExecutedEvent = WorkflowExecutedEvent.createNew(this);
    this.workflowCondition = condition;
    this.active = active;
  }

  public static Workflow createNew(String name, String description, EntityType entityType,
      WorkflowTrigger trigger, User creator, Set<AbstractWorkflowAction> workflowActions,
      WorkflowCondition condition, boolean active) {
    if (!creator.canCreateWorkflow()) {
      log.error("User with id: {} does not have create permission on Workflow", creator.getId());
      throw new InsufficientPrivilegeException();
    }
    validateProperties(entityType, trigger, workflowActions, condition);
    return new Workflow(name, description, entityType, trigger, workflowActions, condition, creator.getTenantId(), creator, creator, active);
  }

  private static void validateProperties(EntityType entityType, WorkflowTrigger trigger, Set<AbstractWorkflowAction> workflowActions,
      WorkflowCondition condition) {
    if (entityType == null) {
      log.error("Try to create workflow with entityType {}", entityType);
      throw new InvalidWorkflowPropertyException();
    }
    if (trigger == null) {
      log.error("Try to create workflow with trigger {}", trigger);
      throw new InvalidWorkflowPropertyException();
    }
    if (isEmpty(workflowActions)) {
      log.error("Try to create workflow with action {}", workflowActions);
      throw new InvalidWorkflowPropertyException();
    }
    if (condition == null) {
      log.error("Try to create workflow with condition {}", condition);
      throw new InvalidWorkflowPropertyException();
    }
  }

  public Workflow setAllowedActionsForUser(User loggedInUser) {
    this.allowedActions = whatActionsCanUserTake(loggedInUser);
    return this;
  }

  private Action whatActionsCanUserTake(User user) {
    var action = new Action();
    if (!belongsToSameTenant(user)) {
      return action;
    }
    if (user.canQueryAllWorkflow() || user.canQueryHisWorkflow() && isCreator(user)) {
      action.setRead(true);
    }
    if (isSystemDefault()) {
      return action;
    }
    if (user.canUpdateAllWorkflow() || user.canUpdateHisWorkflow() && isCreator(user)) {
      action.setUpdate(true);
    }
    if (user.canCreateWorkflow()) {
      action.setWrite(true);
    }
    return action;
  }

  private boolean belongsToSameTenant(User user) {
    return user.getTenantId() == this.tenantId;
  }

  private boolean isCreator(User user) {
    return this.createdBy.getId() == user.getId();
  }

  public Workflow activate() {
    return new Workflow(
        this.id,
        this.name,
        this.description,
        this.entityType,
        this.workflowTrigger,
        this.workflowCondition,
        this.workflowActions,
        true,
        this.createdBy,
        this.updatedBy,
        this.tenantId,
        this.createdAt,
        new Date(),
        this.allowedActions,
        this.workflowExecutedEvent,
        this.systemDefault);
  }

  public Workflow deactivate() {
    return new Workflow(
        this.id,
        this.name,
        this.description,
        this.entityType,
        this.workflowTrigger,
        this.workflowCondition,
        this.workflowActions,
        false,
        this.createdBy,
        this.updatedBy,
        this.tenantId,
        this.createdAt,
        new Date(),
        this.allowedActions,
        this.workflowExecutedEvent,
        this.systemDefault);
  }

  public Workflow update(
      String name, String description, EntityType entityType, WorkflowCondition condition, WorkflowTrigger trigger,
      Set<AbstractWorkflowAction> actions, User updatedBy) {
    if (!updatedBy.canUpdateAllWorkflow()) {
      log.error("User with id: {} does not have update permission on Workflow", updatedBy.getId());
      throw new InsufficientPrivilegeException();
    }
    validateProperties(entityType, trigger, actions, condition);
    return new Workflow(
        this.id,
        name,
        description,
        entityType,
        trigger,
        condition,
        actions,
        this.active,
        this.createdBy,
        updatedBy,
        this.tenantId,
        this.createdAt,
        new Date(),
        this.allowedActions,
        this.workflowExecutedEvent,
        this.systemDefault
    );
  }

  public Workflow withSystemDefault(boolean systemDefault) {
    return new Workflow(
        this.id,
        this.name,
        this.description,
        this.entityType,
        this.workflowTrigger,
        this.workflowCondition,
        this.workflowActions,
        this.active,
        this.createdBy,
        this.updatedBy,
        this.tenantId,
        this.createdAt,
        new Date(),
        this.allowedActions,
        this.workflowExecutedEvent,
        systemDefault);
  }
}
