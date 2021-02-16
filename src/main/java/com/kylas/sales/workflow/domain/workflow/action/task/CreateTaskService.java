package com.kylas.sales.workflow.domain.workflow.action.task;

import com.kylas.sales.workflow.domain.processor.task.Metadata;
import com.kylas.sales.workflow.domain.processor.task.TaskRelation;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.mq.CreateTaskEventPublisher;
import com.kylas.sales.workflow.mq.event.CreateTaskEvent;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CreateTaskService {

  private final CreateTaskEventPublisher createTaskEventPublisher;

  @Autowired
  public CreateTaskService(CreateTaskEventPublisher createTaskEventPublisher) {
    this.createTaskEventPublisher = createTaskEventPublisher;
  }

  public void processCreateTaskAction(CreateTaskAction createTaskAction, EntityType entityType, long entityId, Metadata metadata) {

    Date updatedDueDate = getDueDate(createTaskAction.getDueDate());

    CreateTaskEvent createTaskEvent = new CreateTaskEvent(createTaskAction.getName(), createTaskAction.getDescription(),
        createTaskAction.getPriority(), createTaskAction.getOutcome(), createTaskAction.getTaskType(), createTaskAction.getStatus(),
        createTaskAction.getAssignedTo(), Set.of(new TaskRelation(entityType, entityId)), updatedDueDate,
        metadata);
    log.info("publishing create task event for entity {} with entityId {}", entityType, entityId);
    createTaskEventPublisher.publishCreateTaskEvent(createTaskEvent);
  }

  private Date getDueDate(DueDate dueDate) {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DAY_OF_MONTH, dueDate.getDays());
    calendar.add(Calendar.HOUR_OF_DAY, dueDate.getHours());
    return calendar.getTime();
  }
}
