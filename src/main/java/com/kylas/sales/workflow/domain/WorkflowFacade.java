package com.kylas.sales.workflow.domain;

import com.kylas.sales.workflow.api.request.WorkflowRequest;
import com.kylas.sales.workflow.domain.service.UserService;
import com.kylas.sales.workflow.domain.user.UserFacade;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.WorkflowCondition;
import com.kylas.sales.workflow.domain.workflow.WorkflowTrigger;
import com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction;
import com.kylas.sales.workflow.security.AuthService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class WorkflowFacade {

  private final WorkflowRepository workflowRepository;
  private final AuthService authService;
  private final UserService userService;
  private final UserFacade userFacade;

  @Autowired
  public WorkflowFacade(WorkflowRepository workflowRepository, AuthService authService,
      UserService userService, UserFacade userFacade) {
    this.workflowRepository = workflowRepository;
    this.authService = authService;
    this.userService = userService;
    this.userFacade = userFacade;
  }

  public Mono<Workflow> create(WorkflowRequest workflowRequest) {
    var loggedInUser = authService.getLoggedInUser();
    var authenticationToken = authService.getAuthenticationToken();
    return userService.getUserDetails(loggedInUser.getId(), authenticationToken)
        .map(user -> userFacade.getExistingOrCreateNewUser(user, loggedInUser.getTenantId()))
        .map(user -> {
          var editPropertyActions = EditPropertyAction.createNew(workflowRequest.getActions());
          var condition = WorkflowCondition.createNew(workflowRequest.getCondition());
          var trigger = WorkflowTrigger.createNew(workflowRequest.getTrigger());
          Workflow aNew = Workflow.createNew(
              workflowRequest.getName(),
              workflowRequest.getDescription(),
              workflowRequest.getEntityType(),
              trigger, user, editPropertyActions, condition);
          return workflowRepository.saveAndFlush(aNew);
        });

  }

  public List<Workflow> findAllBy(long tenantId, EntityType entityType) {
    return workflowRepository.findAllByTenantIdAndEntityType(tenantId, entityType);
  }
}
