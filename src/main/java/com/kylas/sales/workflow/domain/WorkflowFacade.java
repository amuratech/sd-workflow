package com.kylas.sales.workflow.domain;

import static com.kylas.sales.workflow.domain.WorkflowSpecification.belongToTenant;
import static com.kylas.sales.workflow.domain.WorkflowSpecification.belongToUser;
import static com.kylas.sales.workflow.domain.WorkflowSpecification.withEntityType;
import static com.kylas.sales.workflow.domain.WorkflowSpecification.withId;

import com.kylas.sales.workflow.api.request.WorkflowRequest;
import com.kylas.sales.workflow.domain.exception.InsufficientPrivilegeException;
import com.kylas.sales.workflow.domain.exception.WorkflowNotFoundException;
import com.kylas.sales.workflow.domain.service.UserService;
import com.kylas.sales.workflow.domain.user.User;
import com.kylas.sales.workflow.domain.user.UserFacade;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.WorkflowCondition;
import com.kylas.sales.workflow.domain.workflow.WorkflowTrigger;
import com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction;
import com.kylas.sales.workflow.security.AuthService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
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
    return workflowRepository.findAll(belongToTenant(tenantId).and(withEntityType(entityType)));
  }

  public Workflow get(long workflowId) {
    User loggedInUser = authService.getLoggedInUser();

    Specification<Workflow> readSpecification = getSpecificationByReadPrivileges(loggedInUser)
        .and(withId(workflowId));
    return workflowRepository.findOne(readSpecification)
        .map(workflow -> workflow.setAllowedActionsForUser(loggedInUser))
        .orElseThrow(WorkflowNotFoundException::new);
  }

  private Specification<Workflow> getSpecificationByReadPrivileges(User user) {
    if (!user.canQueryHisWorkflow() && !user.canQueryAllWorkflow()) {
      log.error("TenantId {} and UserId {} does not have read privilege on workflow", user.getTenantId(),
          user.getId());
      throw new InsufficientPrivilegeException();
    }
    Specification<Workflow> withTenant = belongToTenant(user.getTenantId());
    if (user.canQueryAllWorkflow()) {
      return withTenant;
    }
    return withTenant.and(belongToUser(user.getId()));
  }
}
