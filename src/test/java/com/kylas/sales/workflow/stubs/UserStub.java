package com.kylas.sales.workflow.stubs;

import com.kylas.sales.workflow.domain.user.Action;
import com.kylas.sales.workflow.domain.user.Permission;
import com.kylas.sales.workflow.domain.user.User;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserStub {

  public static User aUser(
      long userId,
      long tenantId,
      boolean canCreateWorkflow,
      boolean canReadWorkflow,
      boolean canReadAllWorkflows,
      boolean canUpdateWorkflow,
      boolean canUpdateAllWorkflows) {
    var action = new Action();
    action.setWrite(canCreateWorkflow);
    action.setRead(canReadWorkflow);
    action.setReadAll(canReadAllWorkflows);
    action.setUpdate(canUpdateWorkflow);
    action.setUpdateAll(canUpdateAllWorkflows);
    var permission = new Permission(11, "workflow", "Workflow Service", action);
    return new User(userId, tenantId, Stream.of(permission).collect(Collectors.toSet()));
  }
}
