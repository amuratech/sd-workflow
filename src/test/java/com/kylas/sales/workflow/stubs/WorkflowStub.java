package com.kylas.sales.workflow.stubs;

import com.kylas.sales.workflow.api.request.WorkflowRequest;
import com.kylas.sales.workflow.api.response.WorkflowDetail;
import com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction;
import com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.ValueType;
import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.common.dto.User;
import com.kylas.sales.workflow.common.dto.WorkflowCondition;
import com.kylas.sales.workflow.common.dto.WorkflowCondition.ConditionExpression;
import com.kylas.sales.workflow.common.dto.WorkflowTrigger;
import com.kylas.sales.workflow.domain.user.Action;
import com.kylas.sales.workflow.domain.workflow.ConditionType;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.TriggerFrequency;
import com.kylas.sales.workflow.domain.workflow.TriggerType;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
      boolean active
  ) {
    var triggerRequest = new WorkflowTrigger(triggerType, triggerFrequency);
    var conditionRequest = new WorkflowCondition(conditionType);
    var actionRequest = new ActionResponse(actionType, new EditPropertyAction(propertyName, propertyValue, propertyValueType));
    var actionRequests = new HashSet<ActionResponse>();
    actionRequests.add(actionRequest);
    return new WorkflowRequest(name, description, entityType, triggerRequest, conditionRequest, actionRequests, active);
  }

  public static WorkflowRequest anConditionBasedEditPropertyWorkflowRequest(
      String name,
      String description,
      EntityType entityType,
      ConditionExpression expression,
      Set<ActionResponse> actions) {
    var triggerRequest = new WorkflowTrigger(TriggerType.EVENT, TriggerFrequency.CREATED);
    var conditionRequest = new WorkflowCondition(ConditionType.CONDITION_BASED, expression);
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
    var conditionRequest = new WorkflowCondition(conditionType);
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
    var conditionRequest = new WorkflowCondition(conditionType);
    return new WorkflowRequest(name, description, entityType, triggerRequest, conditionRequest, actions, active);
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
      boolean active
  ) {
    var triggerRequest = new WorkflowTrigger(triggerType, triggerFrequency);
    var conditionRequest = new WorkflowCondition(conditionType);
    var actionRequest = new ActionResponse(actionId, actionType, new EditPropertyAction(propertyName, propertyValue, propertyValueType));
    var actionRequests = new HashSet<ActionResponse>();
    actionRequests.add(actionRequest);
    return new WorkflowRequest(name, description, entityType, triggerRequest, conditionRequest, actionRequests, active);
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
      String propertyValue,
      ValueType propertyValueType,
      boolean canCreate, boolean canRead, User createdBy, User updatedBy, Date createdAndUpdatedAt
  ) {
    var workflowTrigger = new WorkflowTrigger(triggerType, triggerFrequency);
    var condition = new WorkflowCondition(conditionType);
    var action = new ActionResponse(actionType, new EditPropertyAction(propertyName, propertyValue, propertyValueType));
    var actions = new HashSet<ActionResponse>();
    actions.add(action);
    Action allowedActions = new Action();
    allowedActions.setRead(canRead);
    allowedActions.setWrite(canCreate);
    return new WorkflowDetail(id, name, description, entityType, workflowTrigger, condition, actions, createdBy, updatedBy, createdAndUpdatedAt,
        createdAndUpdatedAt, null, 0L, allowedActions, active);
  }

}
