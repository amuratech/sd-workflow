package com.kylas.sales.workflow.domain.workflow.action.task;

import static com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType.CREATE_TASK;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.kylas.sales.workflow.common.dto.ActionDetail;
import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.domain.exception.InvalidWorkflowRequestException;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.action.AbstractWorkflowAction;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class CreateTaskAction extends AbstractWorkflowAction implements WorkflowAction {

  @NotBlank
  private String name;
  private String description;
  private Long priority;
  private String outcome;
  @Column(name = "type")
  private long taskType;
  private long status;
  private long assignedTo;

  @Embedded
  @NotNull
  @Valid
  @AttributeOverrides(
      value = {
          @AttributeOverride(
              name = "days",
              column = @Column(name = "due_days")),
          @AttributeOverride(name = "hours", column = @Column(name = "due_hours"))
      })
  private DueDate dueDate;

  public CreateTaskAction(@NotBlank String name, String description, Long priority, String outcome, long taskType, long status, long assignedTo,
      @NotNull DueDate dueDate) {
    this.name = name;
    this.description = description;
    this.priority = priority;
    this.outcome = outcome;
    this.taskType = taskType;
    this.status = status;
    this.assignedTo = assignedTo;
    this.dueDate = dueDate;
  }

  public static AbstractWorkflowAction createNew(ActionResponse action) {
    return CreateTaskActionMapper.fromActionResponse(action);
  }

  public static ActionResponse toActionResponse(CreateTaskAction workflowAction) {
    return new ActionResponse(workflowAction.getId(), workflowAction.getType(), CreateTaskActionMapper.fromWorkflowAction(workflowAction));
  }

  @Override
  public AbstractWorkflowAction update(ActionResponse action) {
    var payload = (ActionDetail.CreateTaskAction) action.getPayload();
    CreateTaskActionMapper.validate(payload);
    this.setName(payload.getName());
    this.setDescription(payload.getDescription());
    this.setPriority(payload.getPriority());
    this.setOutcome(payload.getOutcome());
    this.setTaskType(payload.getType());
    this.setStatus(payload.getStatus());
    this.setAssignedTo(payload.getAssignedTo());
    this.setDueDate(payload.getDueDate());
    return this;
  }

  @Override
  public void setWorkflow(Workflow workflow) {
    super.setWorkflow(workflow);
  }

  @Override
  public ActionType getType() {
    return CREATE_TASK;
  }

  @Component
  private static class CreateTaskActionMapper {

    private static CreateTaskAction fromActionResponse(ActionResponse actionResponse) {
      ActionDetail.CreateTaskAction payload = (ActionDetail.CreateTaskAction) actionResponse.getPayload();
      validate(payload);
      return new CreateTaskAction(payload.getName(), payload.getDescription(), payload.getPriority(),
          payload.getOutcome(), payload.getType(), payload.getStatus(), payload.getAssignedTo(), payload.getDueDate());
    }

    private static ActionDetail.CreateTaskAction fromWorkflowAction(CreateTaskAction workflowAction) {

      return new ActionDetail.CreateTaskAction(workflowAction.getName(), workflowAction.getDescription(), workflowAction.getPriority(),
          workflowAction.getOutcome(), workflowAction.getTaskType(), workflowAction.getStatus(), workflowAction.getAssignedTo(),
          workflowAction.getDueDate());
    }

    private static void validate(ActionDetail.CreateTaskAction payload) {
      if (isBlank(payload.getName()) || !ObjectUtils
          .allNotNull(payload.getType(), payload.getStatus(), payload.getAssignedTo(), payload.getDueDate())) {
        throw new InvalidWorkflowRequestException();
      }
    }
  }
}
