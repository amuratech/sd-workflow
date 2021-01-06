package com.kylas.sales.workflow.domain.workflow.action.reassign;

import static com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType.REASSIGN;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.kylas.sales.workflow.common.dto.ActionDetail;
import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.domain.exception.InvalidActionException;
import com.kylas.sales.workflow.domain.service.UserService;
import com.kylas.sales.workflow.domain.user.User;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.action.AbstractWorkflowAction;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction;
import javax.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class ReassignAction extends AbstractWorkflowAction implements WorkflowAction {

  private Long ownerId;
  private String name;

  public ReassignAction(Long ownerId, String name) {
    this.ownerId = ownerId;
    this.name = name;
  }

  public static AbstractWorkflowAction createNew(ActionResponse actionResponse) {
    var payload = (ActionDetail.ReassignAction) actionResponse.getPayload();
    if (isNull(payload.getId()) || isBlank(payload.getName())) {
      throw new InvalidActionException();
    }
    return new ReassignAction(payload.getId(), payload.getName());
  }

  public static Mono<ActionResponse> toActionResponse(ReassignAction action, String authentication) {
    return UserNameResolver.getUserName(action.ownerId, authentication)
        .map(userName -> new ActionResponse(action.getId(), REASSIGN, new ActionDetail.ReassignAction(action.ownerId, userName)));
  }

  @Override
  public void setWorkflow(Workflow workflow) {
    super.setWorkflow(workflow);
  }

  @Override
  public ReassignAction update(ActionResponse actionResponse) {
    var payload = (ActionDetail.ReassignAction) actionResponse.getPayload();
    if (isNull(payload.getId())) {
      throw new InvalidActionException();
    }
    this.setOwnerId(payload.getId());
    this.setName(payload.getName());
    return this;
  }

  @Override
  public ActionType getType() {
    return REASSIGN;
  }

  @Component
  private static class UserNameResolver {

    private static UserService userService;

    public static Mono<String> getUserName(Long userId, String authenticationToken) {
      return userService.getUserDetails(userId, authenticationToken).map(User::getName);
    }

    @Autowired
    public void setUserService(UserService userService) {
      UserNameResolver.userService = userService;
    }

  }
}
