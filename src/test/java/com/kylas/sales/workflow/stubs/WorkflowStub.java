package com.kylas.sales.workflow.stubs;

import com.kylas.sales.workflow.api.request.Condition;
import com.kylas.sales.workflow.api.request.Condition.ExpressionElement;
import com.kylas.sales.workflow.api.request.WorkflowRequest;
import com.kylas.sales.workflow.api.response.WorkflowDetail;
import com.kylas.sales.workflow.api.response.WorkflowEntry;
import com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction;
import com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.ValueType;
import com.kylas.sales.workflow.common.dto.ActionDetail.ReassignAction;
import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.common.dto.User;
import com.kylas.sales.workflow.common.dto.WorkflowTrigger;
import com.kylas.sales.workflow.domain.user.Action;
import com.kylas.sales.workflow.domain.workflow.ConditionType;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.TriggerFrequency;
import com.kylas.sales.workflow.domain.workflow.TriggerType;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.validation.constraints.NotNull;

public class WorkflowStub {

  public static WorkflowRequest anEditPropertyWorkflowRequest(
      String name,
      String description,
      EntityType entityType,
      TriggerType triggerType,
      TriggerFrequency triggerFrequency,
      ConditionType conditionType,
      ActionType actionType,
      String propertyName,
      String propertyValue,
      ValueType propertyValueType,
      boolean isStandard,
      boolean active
  ) {
    var triggerRequest = new WorkflowTrigger(triggerType, triggerFrequency);
    var actionRequest = new ActionResponse(actionType, new EditPropertyAction(propertyName, propertyValue, propertyValueType, isStandard));
    var actionRequests = new HashSet<ActionResponse>();
    actionRequests.add(actionRequest);
    @NotNull Condition conditionRequest = new Condition(conditionType.name(), null);
    return new WorkflowRequest(name, description, entityType, triggerRequest, conditionRequest, actionRequests, active);
  }

  public static WorkflowRequest anConditionBasedEditPropertyWorkflowRequest(
      String name,
      String description,
      EntityType entityType,
      List<ExpressionElement> conditions,
      Set<ActionResponse> actions) {
    var triggerRequest = new WorkflowTrigger(TriggerType.EVENT, TriggerFrequency.CREATED);
    @NotNull Condition conditionRequest = new Condition("CONDITION_BASED", conditions);
    return new WorkflowRequest(name, description, entityType, triggerRequest, conditionRequest, actions, true);
  }

  public static WorkflowRequest aWorkflowRequestWithActions(
      String name,
      String description,
      EntityType entityType,
      TriggerType triggerType,
      TriggerFrequency triggerFrequency,
      ConditionType conditionType,
      Set<ActionResponse> actionRequests) {
    var triggerRequest = new WorkflowTrigger(triggerType, triggerFrequency);
    @NotNull Condition conditionRequest = new Condition(conditionType.name(), null);
    return new WorkflowRequest(name, description, entityType, triggerRequest, conditionRequest, actionRequests, true);
  }

  public static WorkflowRequest aWorkflowRequestWithActions(
      String name,
      String description,
      EntityType entityType,
      TriggerType triggerType,
      TriggerFrequency triggerFrequency,
      ConditionType conditionType,
      boolean active,
      Set<ActionResponse> actions) {
    var triggerRequest = new WorkflowTrigger(triggerType, triggerFrequency);
    @NotNull Condition condition = new Condition(conditionType.name(), null);
    return new WorkflowRequest(name, description, entityType, triggerRequest, condition, actions, active);
  }

  public static WorkflowRequest anExistingEditPropertyWorkflowRequest(
      String name,
      String description,
      EntityType entityType,
      TriggerType triggerType,
      TriggerFrequency triggerFrequency,
      ConditionType conditionType,
      UUID actionId,
      ActionType actionType,
      String propertyName,
      String propertyValue,
      ValueType propertyValueType,
      boolean isStandard,
      boolean active
  ) {
    var triggerRequest = new WorkflowTrigger(triggerType, triggerFrequency);
    var condition = new Condition(conditionType.name(), null);
    var actionRequest = new ActionResponse(actionId, actionType, new EditPropertyAction(propertyName, propertyValue, propertyValueType, isStandard));
    var actionRequests = new HashSet<ActionResponse>();
    actionRequests.add(actionRequest);
    return new WorkflowRequest(name, description, entityType, triggerRequest, condition, actionRequests, active);
  }

  public static WorkflowRequest anExistingReassignWorkflowRequest(
      String name,
      String description,
      EntityType entityType,
      TriggerType triggerType,
      TriggerFrequency triggerFrequency,
      ConditionType conditionType,
      UUID actionId,
      ActionType actionType,
      Long ownerId,
      String userName,
      boolean active
  ) {
    var triggerRequest = new WorkflowTrigger(triggerType, triggerFrequency);
    var actionRequest = new ActionResponse(actionId, actionType, new ReassignAction(ownerId, userName));
    var actionRequests = new HashSet<ActionResponse>();
    actionRequests.add(actionRequest);
    @NotNull Condition condition = new Condition(conditionType.name(), null);
    return new WorkflowRequest(name, description, entityType, triggerRequest, condition, actionRequests, active);
  }

  public static WorkflowDetail workflowDetail(
      long id,
      String name,
      String description,
      EntityType entityType,
      boolean active,
      TriggerType triggerType,
      TriggerFrequency triggerFrequency,
      ConditionType conditionType,
      ActionType actionType,
      String propertyName,
      Object propertyValue,
      ValueType propertyValueType,
      boolean isStandard,
      boolean canCreate, boolean canRead, User createdBy, User updatedBy, Date createdAndUpdatedAt
  ) {
    var workflowTrigger = new WorkflowTrigger(triggerType, triggerFrequency);
    var condition = new Condition(conditionType.name(), null);
    var action = new ActionResponse(actionType, new EditPropertyAction(propertyName, propertyValue, propertyValueType, isStandard));
    var actions = new ArrayList<ActionResponse>();
    actions.add(action);
    Action allowedActions = new Action();
    allowedActions.setRead(canRead);
    allowedActions.setWrite(canCreate);
    return new WorkflowDetail(id, name, description, entityType, workflowTrigger, condition, actions, createdBy, updatedBy, createdAndUpdatedAt,
        createdAndUpdatedAt, null, 0L, allowedActions, active);
  }

  public static WorkflowEntry workflowEntry(
      long id, String name, EntityType entityType, boolean active, boolean canCreate,
      boolean canRead, User createdBy, User updatedBy, Date createdAndUpdatedAt) {
    Action allowedActions = new Action();
    allowedActions.setRead(canRead);
    allowedActions.setWrite(canCreate);
    return new WorkflowEntry(
        id, name, entityType, createdBy, updatedBy, createdAndUpdatedAt,
        createdAndUpdatedAt, null, 0L, allowedActions, active);
  }

}
