package com.kylas.sales.workflow.domain.workflow.action.task;

import static com.kylas.sales.workflow.domain.workflow.EntityType.CONTACT;
import static com.kylas.sales.workflow.domain.workflow.EntityType.DEAL;
import static com.kylas.sales.workflow.domain.workflow.EntityType.LEAD;
import static org.assertj.core.api.Assertions.assertThat;

import com.kylas.sales.workflow.domain.processor.contact.ContactDetail;
import com.kylas.sales.workflow.domain.processor.deal.DealDetail;
import com.kylas.sales.workflow.domain.processor.lead.IdName;
import com.kylas.sales.workflow.domain.processor.lead.LeadDetail;
import com.kylas.sales.workflow.domain.processor.task.AssignedToType;
import com.kylas.sales.workflow.domain.processor.task.Metadata;
import com.kylas.sales.workflow.mq.CreateTaskEventPublisher;
import com.kylas.sales.workflow.mq.event.CreateTaskEvent;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class CreateTaskServiceTest {

  @InjectMocks
  private CreateTaskService createTaskService;
  @Mock
  private CreateTaskEventPublisher createTaskEventPublisher;

  @Test
  public void givenCreateTaskAction_withEntityLead_shouldProcessIt() {

    //given
    var entityId = 100L;
    var userId = 10L;
    var tenantId = 20L;
    var createTaskAction = new CreateTaskAction("New Task", "My description", 1L, "called", 2L, 3L,
        new AssignedTo(AssignedToType.USER, 1L, "James Bond"), new DueDate(2, 4));
    var leadDetail = new LeadDetail();
    leadDetail.setId(entityId);
    //when
    createTaskService.processCreateTaskAction(createTaskAction, LEAD, leadDetail, new Metadata(userId, tenantId));
    //then
    ArgumentCaptor<CreateTaskEvent> createTaskEventArgumentCaptor = ArgumentCaptor.forClass(CreateTaskEvent.class);
    Mockito.verify(createTaskEventPublisher, Mockito.times(1)).publishCreateTaskEvent(createTaskEventArgumentCaptor.capture());
    CreateTaskEvent createTaskEvent = createTaskEventArgumentCaptor.getValue();
    assertThat(createTaskEvent.getName()).isEqualTo("New Task");
    assertThat(createTaskEvent.getDescription()).isEqualTo("My description");
    assertThat(createTaskEvent.getPriority()).isEqualTo(1L);
    assertThat(createTaskEvent.getOutcome()).isEqualTo("called");
    assertThat(createTaskEvent.getType()).isEqualTo(2L);
    assertThat(createTaskEvent.getStatus()).isEqualTo(3L);
    assertThat(createTaskEvent.getAssignedTo()).isEqualTo(1L);
    assertThat(createTaskEvent.getRelation()).hasSize(1);
    assertThat(createTaskEvent.getRelation())
        .allMatch(taskRelation -> taskRelation.getTargetEntityType().equals(LEAD) && taskRelation.getTargetEntityId() == 100L);
    assertThat(createTaskEvent.getDueDate()).isNotNull();

    long diffInMilliSeconds = Math.abs(createTaskEvent.getDueDate().getTime() - new Date().getTime());
    long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMilliSeconds);

    assertThat(diffInDays).isEqualTo(2);
    assertThat(getDateDifferenceInHours(createTaskEvent.getDueDate())).isGreaterThanOrEqualTo(4);
    assertThat(createTaskEvent.getMetadata().getUserId()).isEqualTo(10L);
    assertThat(createTaskEvent.getMetadata().getTenantId()).isEqualTo(20L);
  }

  @Test
  public void givenCreateTaskAction_withEntityDeal_shouldProcessIt() {

    //given
    var entityId = 100L;
    var userId = 10L;
    var tenantId = 20L;
    var createTaskAction = new CreateTaskAction("New Task", "My description", 1L, "called", 2L, 3L,
        new AssignedTo(AssignedToType.USER, 1L, "James Bond"), new DueDate(2, 4));
    var dealDetail = new DealDetail();
    dealDetail.setId(entityId);
    //when
    createTaskService.processCreateTaskAction(createTaskAction, DEAL, dealDetail, new Metadata(userId, tenantId));
    //then
    ArgumentCaptor<CreateTaskEvent> createTaskEventArgumentCaptor = ArgumentCaptor.forClass(CreateTaskEvent.class);
    Mockito.verify(createTaskEventPublisher, Mockito.times(1)).publishCreateTaskEvent(createTaskEventArgumentCaptor.capture());
    CreateTaskEvent createTaskEvent = createTaskEventArgumentCaptor.getValue();
    assertThat(createTaskEvent.getName()).isEqualTo("New Task");
    assertThat(createTaskEvent.getDescription()).isEqualTo("My description");
    assertThat(createTaskEvent.getPriority()).isEqualTo(1L);
    assertThat(createTaskEvent.getOutcome()).isEqualTo("called");
    assertThat(createTaskEvent.getType()).isEqualTo(2L);
    assertThat(createTaskEvent.getStatus()).isEqualTo(3L);
    assertThat(createTaskEvent.getAssignedTo()).isEqualTo(1L);
    assertThat(createTaskEvent.getRelation()).hasSize(1);
    assertThat(createTaskEvent.getRelation())
        .allMatch(taskRelation -> taskRelation.getTargetEntityType().equals(DEAL) && taskRelation.getTargetEntityId() == 100L);
    assertThat(createTaskEvent.getDueDate()).isNotNull();

    long diffInMilliSeconds = Math.abs(createTaskEvent.getDueDate().getTime() - new Date().getTime());
    long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMilliSeconds);

    assertThat(diffInDays).isEqualTo(2);
    assertThat(getDateDifferenceInHours(createTaskEvent.getDueDate())).isGreaterThanOrEqualTo(4);
    assertThat(createTaskEvent.getMetadata().getUserId()).isEqualTo(10L);
    assertThat(createTaskEvent.getMetadata().getTenantId()).isEqualTo(20L);
  }

  @Test
  public void givenCreateTaskAction_withEntityContact_shouldProcessIt() {

    //given
    var entityId = 100L;
    var userId = 10L;
    var tenantId = 20L;
    var createTaskAction = new CreateTaskAction("New Task", "My description", 1L, "called", 2L, 3L,
        new AssignedTo(AssignedToType.USER, 1L, "James Bond"), new DueDate(2, 4));
    var contactDetail = new ContactDetail();
    contactDetail.setId(entityId);
    //when
    createTaskService.processCreateTaskAction(createTaskAction, CONTACT, contactDetail, new Metadata(userId, tenantId));
    //then
    ArgumentCaptor<CreateTaskEvent> createTaskEventArgumentCaptor = ArgumentCaptor.forClass(CreateTaskEvent.class);
    Mockito.verify(createTaskEventPublisher, Mockito.times(1)).publishCreateTaskEvent(createTaskEventArgumentCaptor.capture());
    CreateTaskEvent createTaskEvent = createTaskEventArgumentCaptor.getValue();
    assertThat(createTaskEvent.getName()).isEqualTo("New Task");
    assertThat(createTaskEvent.getDescription()).isEqualTo("My description");
    assertThat(createTaskEvent.getPriority()).isEqualTo(1L);
    assertThat(createTaskEvent.getOutcome()).isEqualTo("called");
    assertThat(createTaskEvent.getType()).isEqualTo(2L);
    assertThat(createTaskEvent.getStatus()).isEqualTo(3L);
    assertThat(createTaskEvent.getAssignedTo()).isEqualTo(1L);
    assertThat(createTaskEvent.getRelation()).hasSize(1);
    assertThat(createTaskEvent.getRelation())
        .allMatch(taskRelation -> taskRelation.getTargetEntityType().equals(CONTACT) && taskRelation.getTargetEntityId() == 100L);
    assertThat(createTaskEvent.getDueDate()).isNotNull();

    long diffInMilliSeconds = Math.abs(createTaskEvent.getDueDate().getTime() - new Date().getTime());
    long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMilliSeconds);

    assertThat(diffInDays).isEqualTo(2);
    assertThat(getDateDifferenceInHours(createTaskEvent.getDueDate())).isGreaterThanOrEqualTo(4);
    assertThat(createTaskEvent.getMetadata().getUserId()).isEqualTo(10L);
    assertThat(createTaskEvent.getMetadata().getTenantId()).isEqualTo(20L);
  }

  @Test
  public void givenCreateTaskAction_withAssignedToLeadOwner_shouldProcessIt() {

    //given
    var entityId = 100L;
    var userId = 10L;
    var tenantId = 20L;
    var createTaskAction = new CreateTaskAction("New Task", "My description", 1L, "called", 2L, 3L,
        new AssignedTo(AssignedToType.OWNER, null, "James Bond"), new DueDate(2, 4));
    var leadDetail = new LeadDetail();
    leadDetail.setId(entityId);
    leadDetail.setOwnerId(new IdName(2L, "James Bond"));
    leadDetail.setCreatedBy(new IdName(3L, "James BondCreatedBy"));
    //when
    createTaskService.processCreateTaskAction(createTaskAction, LEAD, leadDetail, new Metadata(userId, tenantId));
    //then
    ArgumentCaptor<CreateTaskEvent> createTaskEventArgumentCaptor = ArgumentCaptor.forClass(CreateTaskEvent.class);
    Mockito.verify(createTaskEventPublisher, Mockito.times(1)).publishCreateTaskEvent(createTaskEventArgumentCaptor.capture());
    CreateTaskEvent createTaskEvent = createTaskEventArgumentCaptor.getValue();
    assertThat(createTaskEvent.getName()).isEqualTo("New Task");
    assertThat(createTaskEvent.getDescription()).isEqualTo("My description");
    assertThat(createTaskEvent.getPriority()).isEqualTo(1L);
    assertThat(createTaskEvent.getOutcome()).isEqualTo("called");
    assertThat(createTaskEvent.getType()).isEqualTo(2L);
    assertThat(createTaskEvent.getStatus()).isEqualTo(3L);
    assertThat(createTaskEvent.getAssignedTo()).isEqualTo(2L);
    assertThat(createTaskEvent.getRelation()).hasSize(1);
    assertThat(createTaskEvent.getRelation())
        .allMatch(taskRelation -> taskRelation.getTargetEntityType().equals(LEAD) && taskRelation.getTargetEntityId() == 100L);
    assertThat(createTaskEvent.getDueDate()).isNotNull();

    long diffInMilliSeconds = Math.abs(createTaskEvent.getDueDate().getTime() - new Date().getTime());
    long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMilliSeconds);

    assertThat(diffInDays).isEqualTo(2);
    assertThat(getDateDifferenceInHours(createTaskEvent.getDueDate())).isGreaterThanOrEqualTo(4);
    assertThat(createTaskEvent.getMetadata().getUserId()).isEqualTo(10L);
    assertThat(createTaskEvent.getMetadata().getTenantId()).isEqualTo(20L);
  }

  @Test
  public void givenCreateTaskAction_withAssignedToLeadCreatedBy_shouldProcessIt() {

    //given
    var entityId = 100L;
    var userId = 10L;
    var tenantId = 20L;
    var createTaskAction = new CreateTaskAction("New Task", "My description", 1L, "called", 2L, 3L,
        new AssignedTo(AssignedToType.CREATED_BY, null, "James Bond"), new DueDate(2, 4));
    var leadDetail = new LeadDetail();
    leadDetail.setId(entityId);
    leadDetail.setOwnerId(new IdName(2L, "James Bond"));
    leadDetail.setCreatedBy(new IdName(3L, "James BondCreatedBy"));
    //when
    createTaskService.processCreateTaskAction(createTaskAction, LEAD, leadDetail, new Metadata(userId, tenantId));
    //then
    ArgumentCaptor<CreateTaskEvent> createTaskEventArgumentCaptor = ArgumentCaptor.forClass(CreateTaskEvent.class);
    Mockito.verify(createTaskEventPublisher, Mockito.times(1)).publishCreateTaskEvent(createTaskEventArgumentCaptor.capture());
    CreateTaskEvent createTaskEvent = createTaskEventArgumentCaptor.getValue();
    assertThat(createTaskEvent.getName()).isEqualTo("New Task");
    assertThat(createTaskEvent.getDescription()).isEqualTo("My description");
    assertThat(createTaskEvent.getPriority()).isEqualTo(1L);
    assertThat(createTaskEvent.getOutcome()).isEqualTo("called");
    assertThat(createTaskEvent.getType()).isEqualTo(2L);
    assertThat(createTaskEvent.getStatus()).isEqualTo(3L);
    assertThat(createTaskEvent.getAssignedTo()).isEqualTo(3L);
    assertThat(createTaskEvent.getRelation()).hasSize(1);
    assertThat(createTaskEvent.getRelation())
        .allMatch(taskRelation -> taskRelation.getTargetEntityType().equals(LEAD) && taskRelation.getTargetEntityId() == 100L);
    assertThat(createTaskEvent.getDueDate()).isNotNull();

    long diffInMilliSeconds = Math.abs(createTaskEvent.getDueDate().getTime() - new Date().getTime());
    long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMilliSeconds);

    assertThat(diffInDays).isEqualTo(2);
    assertThat(getDateDifferenceInHours(createTaskEvent.getDueDate())).isGreaterThanOrEqualTo(4);
    assertThat(createTaskEvent.getMetadata().getUserId()).isEqualTo(10L);
    assertThat(createTaskEvent.getMetadata().getTenantId()).isEqualTo(20L);
  }

  @Test
  public void givenCreateTaskAction_withAssignedContactOwner_shouldProcessIt() {

    //given
    var entityId = 100L;
    var userId = 10L;
    var tenantId = 20L;
    var createTaskAction = new CreateTaskAction("New Task", "My description", 1L, "called", 2L, 3L,
        new AssignedTo(AssignedToType.OWNER, null, "James Bond"), new DueDate(2, 4));
    var contactDetail = new ContactDetail();
    contactDetail.setId(entityId);
    contactDetail.setOwnerId(new IdName(4L, "James Bond"));
    contactDetail.setCreatedBy(new IdName(5L, "James BondCreatedBy"));
    //when
    createTaskService.processCreateTaskAction(createTaskAction, CONTACT, contactDetail, new Metadata(userId, tenantId));
    //then
    ArgumentCaptor<CreateTaskEvent> createTaskEventArgumentCaptor = ArgumentCaptor.forClass(CreateTaskEvent.class);
    Mockito.verify(createTaskEventPublisher, Mockito.times(1)).publishCreateTaskEvent(createTaskEventArgumentCaptor.capture());
    CreateTaskEvent createTaskEvent = createTaskEventArgumentCaptor.getValue();
    assertThat(createTaskEvent.getName()).isEqualTo("New Task");
    assertThat(createTaskEvent.getDescription()).isEqualTo("My description");
    assertThat(createTaskEvent.getPriority()).isEqualTo(1L);
    assertThat(createTaskEvent.getOutcome()).isEqualTo("called");
    assertThat(createTaskEvent.getType()).isEqualTo(2L);
    assertThat(createTaskEvent.getStatus()).isEqualTo(3L);
    assertThat(createTaskEvent.getAssignedTo()).isEqualTo(4L);
    assertThat(createTaskEvent.getRelation()).hasSize(1);
    assertThat(createTaskEvent.getRelation())
        .allMatch(taskRelation -> taskRelation.getTargetEntityType().equals(CONTACT) && taskRelation.getTargetEntityId() == 100L);
    assertThat(createTaskEvent.getDueDate()).isNotNull();

    long diffInMilliSeconds = Math.abs(createTaskEvent.getDueDate().getTime() - new Date().getTime());
    long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMilliSeconds);

    assertThat(diffInDays).isEqualTo(2);
    assertThat(getDateDifferenceInHours(createTaskEvent.getDueDate())).isGreaterThanOrEqualTo(4);
    assertThat(createTaskEvent.getMetadata().getUserId()).isEqualTo(10L);
    assertThat(createTaskEvent.getMetadata().getTenantId()).isEqualTo(20L);
  }

  @Test
  public void givenCreateTaskAction_withAssignedToContactCreatedBy_shouldProcessIt() {

    //given
    var entityId = 100L;
    var userId = 10L;
    var tenantId = 20L;
    var createTaskAction = new CreateTaskAction("New Task", "My description", 1L, "called", 2L, 3L,
        new AssignedTo(AssignedToType.CREATED_BY, null, "James Bond"), new DueDate(2, 4));
    var contactDetail = new ContactDetail();
    contactDetail.setId(entityId);
    contactDetail.setOwnerId(new IdName(4L, "James Bond"));
    contactDetail.setCreatedBy(new IdName(5L, "James BondCreatedBy"));
    //when
    createTaskService.processCreateTaskAction(createTaskAction, CONTACT, contactDetail, new Metadata(userId, tenantId));
    //then
    ArgumentCaptor<CreateTaskEvent> createTaskEventArgumentCaptor = ArgumentCaptor.forClass(CreateTaskEvent.class);
    Mockito.verify(createTaskEventPublisher, Mockito.times(1)).publishCreateTaskEvent(createTaskEventArgumentCaptor.capture());
    CreateTaskEvent createTaskEvent = createTaskEventArgumentCaptor.getValue();
    assertThat(createTaskEvent.getName()).isEqualTo("New Task");
    assertThat(createTaskEvent.getDescription()).isEqualTo("My description");
    assertThat(createTaskEvent.getPriority()).isEqualTo(1L);
    assertThat(createTaskEvent.getOutcome()).isEqualTo("called");
    assertThat(createTaskEvent.getType()).isEqualTo(2L);
    assertThat(createTaskEvent.getStatus()).isEqualTo(3L);
    assertThat(createTaskEvent.getAssignedTo()).isEqualTo(5L);
    assertThat(createTaskEvent.getRelation()).hasSize(1);
    assertThat(createTaskEvent.getRelation())
        .allMatch(taskRelation -> taskRelation.getTargetEntityType().equals(CONTACT) && taskRelation.getTargetEntityId() == 100L);
    assertThat(createTaskEvent.getDueDate()).isNotNull();

    long diffInMilliSeconds = Math.abs(createTaskEvent.getDueDate().getTime() - new Date().getTime());
    long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMilliSeconds);

    assertThat(diffInDays).isEqualTo(2);
    assertThat(getDateDifferenceInHours(createTaskEvent.getDueDate())).isGreaterThanOrEqualTo(4);
    assertThat(createTaskEvent.getMetadata().getUserId()).isEqualTo(10L);
    assertThat(createTaskEvent.getMetadata().getTenantId()).isEqualTo(20L);
  }

  @Test
  public void givenCreateTaskAction_withAssignedDealOwner_shouldProcessIt() {

    //given
    var entityId = 100L;
    var userId = 10L;
    var tenantId = 20L;
    var createTaskAction = new CreateTaskAction("New Task", "My description", 1L, "called", 2L, 3L,
        new AssignedTo(AssignedToType.OWNER, null, "James Bond"), new DueDate(2, 4));
    var dealDetail = new DealDetail();
    dealDetail.setId(entityId);
    dealDetail.setOwnedBy(new IdName(8L, "James Bond"));
    dealDetail.setCreatedBy(new IdName(9L, "James BondCreatedBy"));
    //when
    createTaskService.processCreateTaskAction(createTaskAction, DEAL, dealDetail, new Metadata(userId, tenantId));
    //then
    ArgumentCaptor<CreateTaskEvent> createTaskEventArgumentCaptor = ArgumentCaptor.forClass(CreateTaskEvent.class);
    Mockito.verify(createTaskEventPublisher, Mockito.times(1)).publishCreateTaskEvent(createTaskEventArgumentCaptor.capture());
    CreateTaskEvent createTaskEvent = createTaskEventArgumentCaptor.getValue();
    assertThat(createTaskEvent.getName()).isEqualTo("New Task");
    assertThat(createTaskEvent.getDescription()).isEqualTo("My description");
    assertThat(createTaskEvent.getPriority()).isEqualTo(1L);
    assertThat(createTaskEvent.getOutcome()).isEqualTo("called");
    assertThat(createTaskEvent.getType()).isEqualTo(2L);
    assertThat(createTaskEvent.getStatus()).isEqualTo(3L);
    assertThat(createTaskEvent.getAssignedTo()).isEqualTo(8L);
    assertThat(createTaskEvent.getRelation()).hasSize(1);
    assertThat(createTaskEvent.getRelation())
        .allMatch(taskRelation -> taskRelation.getTargetEntityType().equals(DEAL) && taskRelation.getTargetEntityId() == 100L);
    assertThat(createTaskEvent.getDueDate()).isNotNull();

    long diffInMilliSeconds = Math.abs(createTaskEvent.getDueDate().getTime() - new Date().getTime());
    long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMilliSeconds);

    assertThat(diffInDays).isEqualTo(2);
    assertThat(getDateDifferenceInHours(createTaskEvent.getDueDate())).isGreaterThanOrEqualTo(4);
    assertThat(createTaskEvent.getMetadata().getUserId()).isEqualTo(10L);
    assertThat(createTaskEvent.getMetadata().getTenantId()).isEqualTo(20L);
  }

  @Test
  public void givenCreateTaskAction_withAssignedToDealCreatedBy_shouldProcessIt() {

    //given
    var entityId = 100L;
    var userId = 10L;
    var tenantId = 20L;
    var createTaskAction = new CreateTaskAction("New Task", "My description", 1L, "called", 2L, 3L,
        new AssignedTo(AssignedToType.CREATED_BY, null, "James Bond"), new DueDate(2, 4));
    var dealDetail = new DealDetail();
    dealDetail.setId(entityId);
    dealDetail.setOwnedBy(new IdName(8L, "James Bond"));
    dealDetail.setCreatedBy(new IdName(9L, "James BondCreatedBy"));
    //when
    createTaskService.processCreateTaskAction(createTaskAction, DEAL, dealDetail, new Metadata(userId, tenantId));
    //then
    ArgumentCaptor<CreateTaskEvent> createTaskEventArgumentCaptor = ArgumentCaptor.forClass(CreateTaskEvent.class);
    Mockito.verify(createTaskEventPublisher, Mockito.times(1)).publishCreateTaskEvent(createTaskEventArgumentCaptor.capture());
    CreateTaskEvent createTaskEvent = createTaskEventArgumentCaptor.getValue();
    assertThat(createTaskEvent.getName()).isEqualTo("New Task");
    assertThat(createTaskEvent.getDescription()).isEqualTo("My description");
    assertThat(createTaskEvent.getPriority()).isEqualTo(1L);
    assertThat(createTaskEvent.getOutcome()).isEqualTo("called");
    assertThat(createTaskEvent.getType()).isEqualTo(2L);
    assertThat(createTaskEvent.getStatus()).isEqualTo(3L);
    assertThat(createTaskEvent.getAssignedTo()).isEqualTo(9L);
    assertThat(createTaskEvent.getRelation()).hasSize(1);
    assertThat(createTaskEvent.getRelation())
        .allMatch(taskRelation -> taskRelation.getTargetEntityType().equals(DEAL) && taskRelation.getTargetEntityId() == 100L);
    assertThat(createTaskEvent.getDueDate()).isNotNull();

    long diffInMilliSeconds = Math.abs(createTaskEvent.getDueDate().getTime() - new Date().getTime());
    long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMilliSeconds);

    assertThat(diffInDays).isEqualTo(2);
    assertThat(getDateDifferenceInHours(createTaskEvent.getDueDate())).isGreaterThanOrEqualTo(4);
    assertThat(createTaskEvent.getMetadata().getUserId()).isEqualTo(10L);
    assertThat(createTaskEvent.getMetadata().getTenantId()).isEqualTo(20L);
  }


  private int getDateDifferenceInHours(Date dueDate) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(dueDate);
    int dueDateHours = calendar.get(Calendar.HOUR_OF_DAY);
    calendar.setTime(new Date());
    int currentDateHours = calendar.get(Calendar.HOUR_OF_DAY);
    return Math.abs(dueDateHours - currentDateHours);
  }
}
