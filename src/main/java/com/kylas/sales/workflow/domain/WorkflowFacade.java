package com.kylas.sales.workflow.domain;

import static com.kylas.sales.workflow.domain.WorkflowSpecification.active;
import static com.kylas.sales.workflow.domain.WorkflowSpecification.belongToTenant;
import static com.kylas.sales.workflow.domain.WorkflowSpecification.belongToUser;
import static com.kylas.sales.workflow.domain.WorkflowSpecification.withEntityType;
import static com.kylas.sales.workflow.domain.WorkflowSpecification.withId;
import static com.kylas.sales.workflow.domain.WorkflowSpecification.withTriggerFrequency;
import static com.kylas.sales.workflow.domain.processor.FieldValueTypeFactory.createByEntityType;
import static com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType.EDIT_PROPERTY;
import static java.util.Objects.isNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.ObjectUtils.allNotNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.kylas.sales.workflow.api.request.WorkflowRequest;
import com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction;
import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.domain.exception.InsufficientPrivilegeException;
import com.kylas.sales.workflow.domain.exception.InvalidActionException;
import com.kylas.sales.workflow.domain.exception.InvalidValueTypeException;
import com.kylas.sales.workflow.domain.exception.InvalidWorkflowRequestException;
import com.kylas.sales.workflow.domain.exception.WorkflowNotFoundException;
import com.kylas.sales.workflow.domain.service.UserService;
import com.kylas.sales.workflow.domain.user.User;
import com.kylas.sales.workflow.domain.user.UserFacade;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.TriggerFrequency;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.WorkflowTrigger;
import com.kylas.sales.workflow.domain.workflow.action.AbstractWorkflowAction;
import com.kylas.sales.workflow.security.AuthService;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
  private final ConditionFacade conditionFacade;

  @Autowired
  public WorkflowFacade(
      WorkflowRepository workflowRepository,
      WorkflowExecutedEventRepository workflowExecutedEventRepository,
      AuthService authService,
      UserService userService,
      UserFacade userFacade,
      ConditionFacade conditionFacade) {
    this.workflowRepository = workflowRepository;
    this.workflowExecutedEventRepository = workflowExecutedEventRepository;
    this.authService = authService;
    this.userService = userService;
    this.userFacade = userFacade;
    this.conditionFacade = conditionFacade;
  }

  public Mono<Workflow> create(WorkflowRequest workflowRequest) {
    var loggedInUser = authService.getLoggedInUser();
    var authenticationToken = authService.getAuthenticationToken();
    return userService
        .getUserDetails(loggedInUser.getId(), authenticationToken)
        .map(user -> userFacade.getExistingOrCreateNewUser(user, loggedInUser.getTenantId()))
        .map(
            user -> {
              var actions =
                  workflowRequest.getActions().stream()
                      .map(workflowAction -> workflowAction.getType().create(workflowAction))
                      .collect(Collectors.toSet());
              var condition = conditionFacade.createFrom(workflowRequest.getCondition());
              var trigger = WorkflowTrigger.createNew(workflowRequest.getTrigger());
              Workflow aNew =
                  Workflow.createNew(
                      workflowRequest.getName(),
                      workflowRequest.getDescription(),
                      workflowRequest.getEntityType(),
                      trigger,
                      user,
                      actions,
                      condition,
                      workflowRequest.isActive());
              return workflowRepository.saveAndFlush(aNew);
            });
  }

  public List<Workflow> findActiveBy(long tenantId, EntityType entityType, TriggerFrequency triggerFrequency) {
    var entitySpecification = withEntityType(entityType);
    var triggerFrequencySpecification = withTriggerFrequency(triggerFrequency);
    var specification = belongToTenant(tenantId).and(entitySpecification.and(active()).and(triggerFrequencySpecification));
    return workflowRepository.findAll(specification);
  }

  public Workflow get(long workflowId) {
    User loggedInUser = authService.getLoggedInUser();

    Specification<Workflow> readSpecification =
        getSpecificationByReadPrivileges(loggedInUser).and(withId(workflowId));
    return workflowRepository
        .findOne(readSpecification)
        .map(workflow -> workflow.setAllowedActionsForUser(loggedInUser))
        .orElseThrow(WorkflowNotFoundException::new);
  }

  public Mono<Workflow> update(long workflowId, WorkflowRequest request) {
    var loggedInUser = authService.getLoggedInUser();
    var authenticationToken = authService.getAuthenticationToken();
    return userService
        .getUserDetails(loggedInUser.getId(), authenticationToken)
        .map(user -> userFacade.getExistingOrCreateNewUser(user, loggedInUser.getTenantId()))
        .map(
            user ->
                workflowRepository
                    .findOne(
                        getSpecificationByUpdatePrivileges(loggedInUser).and(withId(workflowId)))
                    .map(
                        workflow -> {
                          var condition = conditionFacade.update(request.getCondition(), workflow);

                          var trigger = workflow.getWorkflowTrigger().update(request.getTrigger());
                          var actions =
                              updateOrCreateActions(request, workflow);
                          var workflowToUpdate =
                              workflow.update(
                                  request.getName(),
                                  request.getDescription(),
                                  request.getEntityType(),
                                  condition,
                                  trigger,
                                  actions,
                                  loggedInUser);
                          var updatedWorkflow = workflowRepository.saveAndFlush(workflowToUpdate);
                          updatedWorkflow.setAllowedActionsForUser(loggedInUser);
                          return updatedWorkflow;
                        })
                    .orElseThrow(WorkflowNotFoundException::new));
  }

  private Set<AbstractWorkflowAction> updateOrCreateActions(WorkflowRequest request, Workflow workflow) {
    return request.getActions().stream()
        .map(
            requestedAction ->
                workflow.getWorkflowActions().stream()
                    .filter(
                        existingAction -> existingAction.getId().equals(requestedAction.getId()) && existingAction.getType()
                            .equals(requestedAction.getType()))
                    .findFirst()
                    .map(workflowAction -> workflowAction.update(requestedAction))
                    .orElseGet(() -> requestedAction.getType().create(requestedAction)))
        .collect(Collectors.toCollection(HashSet::new));
  }

  private Specification<Workflow> getSpecificationByReadPrivileges(User user) {
    if (!user.canQueryHisWorkflow() && !user.canQueryAllWorkflow()) {
      log.error(
          "TenantId {} and UserId {} does not have read privilege on workflow",
          user.getTenantId(),
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
    workflowList.getContent().stream()
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
      log.error(
          "TenantId {} and UserId {} does not have update privilege on workflow",
          user.getTenantId(),
          user.getId());
      throw new InsufficientPrivilegeException();
    }
    if (user.canUpdateAllWorkflow()) {
      return belongToTenant(user.getTenantId());
    }
    return belongToTenant(user.getTenantId()).and(belongToUser(user.getId()));
  }

  public Page<Workflow> search(Pageable pageable, Optional<Set<WorkflowFilter>> filters) {
    User loggedInUser = authService.getLoggedInUser();

    Specification<Workflow> readSpecification = getSpecificationByReadPrivileges(loggedInUser);

    if(filters.isPresent()){
      Set<WorkflowFilter> workflowFilters = filters.get();
      for (WorkflowFilter workflowFilter : workflowFilters) {
        readSpecification = readSpecification.and(workflowFilter.toSpecification());
      }
    }
    Page<Workflow> workflowList = workflowRepository.findAll(readSpecification, pageable);
    workflowList.getContent().stream()
        .forEach(workflow -> workflow.setAllowedActionsForUser(loggedInUser));
    return workflowList;
  }

  public void validate(WorkflowRequest request) {
    if (!allNotNull(request.getName(), request.getEntityType(), request.getTrigger(), request.getCondition())) {
      throw new InvalidWorkflowRequestException();
    }

    if (!request.getEntityType().isWorkflowEntity()) {
      throw new InvalidWorkflowRequestException();
    }

    if (isEmpty(request.getActions())) {
      throw new InvalidWorkflowRequestException();
    }

    request.getActions()
        .stream()
        .filter(action -> action.getType().equals(EDIT_PROPERTY))
        .forEach(action -> validateFieldValueType(action, request.getEntityType()));

    conditionFacade.validate(request.getCondition());
  }

  public void validateFieldValueType(ActionResponse workflowAction, EntityType entityType) {
    EditPropertyAction editPropertyAction = (EditPropertyAction) workflowAction.getPayload();
    if (isBlank(editPropertyAction.getName()) || isNull(editPropertyAction.getValue())) {
      throw new InvalidActionException();
    }
    boolean isInvalidValueType = createByEntityType(entityType)
        .isInValidValueType(editPropertyAction.getName(), editPropertyAction.getValueType());
    if (isInvalidValueType) {
      throw new InvalidValueTypeException();
    }
  }
}
