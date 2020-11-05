package com.kylas.sales.workflow.domain;

import static com.kylas.sales.workflow.domain.WorkflowSpecification.active;
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
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class WorkflowFacade {

  private final WorkflowRepository workflowRepository;
  private final WorkflowExecutedEventRepository workflowExecutedEventRepository;
  private final AuthService authService;
  private final UserService userService;
  private final UserFacade userFacade;

  @Autowired
  public WorkflowFacade(WorkflowRepository workflowRepository,
      WorkflowExecutedEventRepository workflowExecutedEventRepository, AuthService authService,
      UserService userService, UserFacade userFacade) {
    this.workflowRepository = workflowRepository;
    this.workflowExecutedEventRepository = workflowExecutedEventRepository;
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
              trigger, user, editPropertyActions, condition, workflowRequest.isActive());
          return workflowRepository.saveAndFlush(aNew);
        });

  }

  public List<Workflow> findActiveBy(long tenantId, EntityType entityType) {
    var entitySpecification = withEntityType(entityType);
    var specification = belongToTenant(tenantId).and(entitySpecification.and(active()));
    return workflowRepository.findAll(specification);
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
    if (user.canQueryAllWorkflow()) {
      return belongToTenant(user.getTenantId());
    }

    return belongToTenant(user.getTenantId()).and(belongToUser(user.getId()));
  }

  public Page<Workflow> list(Pageable pageable) {
    User loggedInUser = authService.getLoggedInUser();

    Specification<Workflow> readSpecification = getSpecificationByReadPrivileges(loggedInUser);
    Page<Workflow> workflowList = workflowRepository.findAll(readSpecification, pageable);
    workflowList.getContent()
        .stream()
        .forEach(workflow -> workflow.setAllowedActionsForUser(loggedInUser));
    return workflowList;
  }

  @Transactional
  public void updateExecutedEvent(Workflow workflow) {
    workflowExecutedEventRepository.updateEventDetails(workflow.getWorkflowExecutedEvent().getId());
  }

  public Workflow deactivate(long workflowId) {
    var user = authService.getLoggedInUser();
    return workflowRepository
        .findOne(getSpecificationByUpdatePrivileges(user).and(withId(workflowId)))
        .map(workflow -> workflowRepository.saveAndFlush(workflow.deactivate()))
        .orElseThrow(WorkflowNotFoundException::new);
  }

  public Workflow activate(long workflowId) {
    var user = authService.getLoggedInUser();
    return workflowRepository
        .findOne(getSpecificationByUpdatePrivileges(user).and(withId(workflowId)))
        .map(workflow -> workflowRepository.saveAndFlush(workflow.activate()))
        .orElseThrow(WorkflowNotFoundException::new);
  }

  private Specification<Workflow> getSpecificationByUpdatePrivileges(User user) {
    if (!user.canUpdateHisWorkflow() && !user.canUpdateAllWorkflow()) {
      log.error("TenantId {} and UserId {} does not have update privilege on workflow", user.getTenantId(),
          user.getId());
      throw new InsufficientPrivilegeException();
    }
    if (user.canUpdateAllWorkflow()) {
      return belongToTenant(user.getTenantId());
    }
    return belongToTenant(user.getTenantId()).and(belongToUser(user.getId()));
  }
}
