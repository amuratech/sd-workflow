package com.kylas.sales.workflow.stubs;

import com.kylas.sales.workflow.api.request.WorkflowRequest;
import com.kylas.sales.workflow.api.response.WorkflowDetail;
import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.common.dto.User;
import com.kylas.sales.workflow.common.dto.WorkflowCondition;
import com.kylas.sales.workflow.common.dto.WorkflowEditProperty;
import com.kylas.sales.workflow.common.dto.WorkflowTrigger;
import com.kylas.sales.workflow.domain.user.Action;
import com.kylas.sales.workflow.domain.workflow.ConditionType;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.TriggerFrequency;
import com.kylas.sales.workflow.domain.workflow.TriggerType;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType;
import java.util.Date;
import java.util.HashSet;
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
      boolean active
  ) {
    var triggerRequest = new WorkflowTrigger(triggerType, triggerFrequency);
    var conditionRequest = new WorkflowCondition(conditionType);
    var actionRequest = new ActionResponse(actionType, new WorkflowEditProperty(propertyName, propertyValue));
    var actionRequests = new HashSet<ActionResponse>();
    actionRequests.add(actionRequest);
    return new WorkflowRequest(name, description, entityType, triggerRequest, conditionRequest, actionRequests, active);
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
      boolean active
  ) {
    var triggerRequest = new WorkflowTrigger(triggerType, triggerFrequency);
    var conditionRequest = new WorkflowCondition(conditionType);
    var actionRequest = new ActionResponse(actionId, actionType, new WorkflowEditProperty(propertyName, propertyValue));
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
      boolean canCreate, boolean canRead, User createdBy, User updatedBy, Date createdAndUpdatedAt
  ) {
    var workflowTrigger = new WorkflowTrigger(triggerType, triggerFrequency);
    var condition = new WorkflowCondition(conditionType);
    var action = new ActionResponse(actionType, new WorkflowEditProperty(propertyName, propertyValue));
    var actions = new HashSet<ActionResponse>();
    actions.add(action);
    Action allowedActions = new Action();
    allowedActions.setRead(canRead);
    allowedActions.setWrite(canCreate);
    return new WorkflowDetail(id, name, description, entityType, workflowTrigger, condition, actions, createdBy, updatedBy, createdAndUpdatedAt,
        createdAndUpdatedAt, null, 0L, allowedActions, active);
  }

}
