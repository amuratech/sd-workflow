package com.kylas.sales.workflow.domain.workflow.action.task;

import com.kylas.sales.workflow.domain.exception.InvalidEntityException;
import com.kylas.sales.workflow.domain.processor.EntityDetail;
import com.kylas.sales.workflow.domain.processor.contact.ContactDetail;
import com.kylas.sales.workflow.domain.processor.deal.DealDetail;
import com.kylas.sales.workflow.domain.processor.lead.IdName;
import com.kylas.sales.workflow.domain.processor.lead.LeadDetail;
import com.kylas.sales.workflow.domain.processor.task.AssignedToType;
import com.kylas.sales.workflow.domain.processor.task.Metadata;
import com.kylas.sales.workflow.domain.processor.task.TaskRelation;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.mq.CreateTaskEventPublisher;
import com.kylas.sales.workflow.mq.event.CreateTaskEvent;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CreateTaskService {

  private final CreateTaskEventPublisher createTaskEventPublisher;
  private static final String ENTITY_ID = "entityId";
  private static final String ASSIGNED_TO_ID = "assignedToId";

  @Autowired
  public CreateTaskService(CreateTaskEventPublisher createTaskEventPublisher) {
    this.createTaskEventPublisher = createTaskEventPublisher;
  }

  public void processCreateTaskAction(CreateTaskAction createTaskAction, EntityType entityType, EntityDetail entityDetail, Metadata metadata) {

    Date updatedDueDate = getDueDate(createTaskAction.getDueDate());

    Map<String, Long> entityDetails = getEntityDetails(entityType, entityDetail, createTaskAction.getAssignedTo());
    long entityId = entityDetails.get(ENTITY_ID);
    long assignedToId = entityDetails.get(ASSIGNED_TO_ID);

    CreateTaskEvent createTaskEvent = new CreateTaskEvent(createTaskAction.getName(), createTaskAction.getDescription(),
        createTaskAction.getPriority(), createTaskAction.getOutcome(), createTaskAction.getTaskType(), createTaskAction.getStatus(),
        assignedToId, Set.of(new TaskRelation(entityType, entityId)), updatedDueDate,
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

  private Map<String, Long> getEntityDetails(EntityType entityType, EntityDetail entityDetail, AssignedTo assignedTo) {
    switch (entityType) {
      case LEAD:
        LeadDetail leadDetail = (LeadDetail) entityDetail;
        long assignedToLeadId = assignedTo.getType().equals(AssignedToType.USER) ? assignedTo.getId()
            : getAssignedToFromIdName(assignedTo, leadDetail.getOwnerId(), leadDetail.getCreatedBy());
        return getAssignedToMap(leadDetail.getId(), assignedToLeadId);

      case CONTACT:
        ContactDetail contactDetail = (ContactDetail) entityDetail;
        long assignedToContactId = assignedTo.getType().equals(AssignedToType.USER) ? assignedTo.getId()
            : getAssignedToFromIdName(assignedTo, contactDetail.getOwnerId(), contactDetail.getCreatedBy());
        return getAssignedToMap(contactDetail.getId(), assignedToContactId);

      case DEAL:
        DealDetail dealDetail = (DealDetail) entityDetail;
        long assignedToDealId = assignedTo.getType().equals(AssignedToType.USER) ? assignedTo.getId()
            : getAssignedToFromIdName(assignedTo, dealDetail.getOwnedBy(), dealDetail.getCreatedBy());
        return getAssignedToMap(dealDetail.getId(), assignedToDealId);
      default:
        throw new InvalidEntityException();
    }
  }

  private Long getAssignedToFromIdName(AssignedTo assignedTo, IdName ownerId, IdName createdBy) {
    return assignedTo.getType().equals(AssignedToType.OWNER) ? ownerId.getId() : createdBy.getId();
  }

  private Map<String, Long> getAssignedToMap(long entityId, long assignedToId) {
    Map<String, Long> detailsMap = new HashMap<>();
    detailsMap.put(ENTITY_ID, entityId);
    detailsMap.put(ASSIGNED_TO_ID, assignedToId);
    return detailsMap;
  }
}
