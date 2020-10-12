package com.kylas.sales.workflow.stubs;

import com.kylas.sales.workflow.api.request.ActionRequest;
import com.kylas.sales.workflow.api.request.ConditionRequest;
import com.kylas.sales.workflow.api.request.EditPropertyRequest;
import com.kylas.sales.workflow.api.request.TriggerRequest;
import com.kylas.sales.workflow.api.request.WorkflowRequest;
import com.kylas.sales.workflow.domain.workflow.ConditionType;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.TriggerFrequency;
import com.kylas.sales.workflow.domain.workflow.TriggerType;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType;
import java.util.HashSet;

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
      String propertyValue
  ){
    var triggerRequest = new TriggerRequest(triggerType, triggerFrequency);
    var conditionRequest = new ConditionRequest(conditionType);
    var actionRequest = new ActionRequest(actionType, new EditPropertyRequest(propertyName, propertyValue));
    var actionRequests = new HashSet<ActionRequest>();
    actionRequests.add(actionRequest);
    return new WorkflowRequest(name,description,entityType,triggerRequest,conditionRequest,actionRequests);
  }

}
