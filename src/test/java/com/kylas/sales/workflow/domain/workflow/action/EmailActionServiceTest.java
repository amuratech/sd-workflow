package com.kylas.sales.workflow.domain.workflow.action;

import static com.kylas.sales.workflow.domain.workflow.EntityType.CONTACT;
import static com.kylas.sales.workflow.domain.workflow.EntityType.DEAL;
import static com.kylas.sales.workflow.domain.workflow.EntityType.LEAD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.kylas.sales.workflow.domain.processor.contact.ContactDetail;
import com.kylas.sales.workflow.domain.processor.contact.ContactResponse;
import com.kylas.sales.workflow.domain.processor.deal.DealDetail;
import com.kylas.sales.workflow.domain.processor.lead.Email;
import com.kylas.sales.workflow.domain.processor.lead.EmailType;
import com.kylas.sales.workflow.domain.processor.lead.IdName;
import com.kylas.sales.workflow.domain.processor.lead.LeadDetail;
import com.kylas.sales.workflow.domain.service.ContactService;
import com.kylas.sales.workflow.domain.service.UserService;
import com.kylas.sales.workflow.domain.user.User;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.action.email.EmailAction;
import com.kylas.sales.workflow.domain.workflow.action.email.EmailActionService;
import com.kylas.sales.workflow.domain.workflow.action.email.EmailActionType;
import com.kylas.sales.workflow.domain.workflow.action.email.EmailEntityType;
import com.kylas.sales.workflow.domain.workflow.action.email.Participant;
import com.kylas.sales.workflow.mq.EmailActionEventPublisher;
import com.kylas.sales.workflow.mq.event.ContactEvent;
import com.kylas.sales.workflow.mq.event.DealEvent;
import com.kylas.sales.workflow.mq.event.EmailActionEvent;
import com.kylas.sales.workflow.mq.event.EntityAction;
import com.kylas.sales.workflow.mq.event.LeadEvent;
import com.kylas.sales.workflow.mq.event.Metadata;
import com.kylas.sales.workflow.security.AuthService;
import com.kylas.sales.workflow.stubs.UserStub;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
public class EmailActionServiceTest {

  @InjectMocks
  private EmailActionService emailActionService;
  @Mock
  private EmailActionEventPublisher emailActionEventPublisher;
  @Mock
  private UserService userService;
  @Mock
  private AuthService authService;
  @Captor
  private ArgumentCaptor<EmailActionEvent> emailActionEventArgumentCaptor;
  @Mock
  private ContactService contactService;

  @Test
  public void givenEmailActionOnLeadEvent_fromRecordOwnerToParticipants_shouldProcessItAndPublishEvent() throws IOException {
    //given
    Object from = getFrom(EmailActionType.RECORD_OWNER.name(), 1L, "record owner", "record.owner@gmail.com");
    List<Participant> participants = List
        .of(new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.USER.getEntityName(), 1L, "test user", "test@user.com"),
            new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.USER.getEntityName(), 2L, "test user", "test@user.com"));

    var lead = new LeadDetail();
    lead.setName("new lead");
    lead.setOwnerId(new IdName(20L, "lead owner"));
    lead.setEmails(
        List.of(new Email(EmailType.PERSONAL, "abc@primary.com", true), new Email(EmailType.OFFICE, "abc@other.com", false)).toArray(Email[]::new));
    lead.setId(55L);
    Metadata metadata = new Metadata(10L, 5L, LEAD, null, null, EntityAction.CREATED);

    var leadEvent = new LeadEvent(lead, null, metadata);
    EmailAction emailAction = new EmailAction(1L, from, participants, participants, participants);
    User aUser = UserStub.aUser(20L, 5L, true, true, true, true, true)
        .withName("Updated leadOwner");

    given(userService.getUserDetails(20L, authService.getAuthenticationToken())).willReturn(Mono.just(aUser));
    //when
    emailActionService.processEmailAction(emailAction, leadEvent);

    //then
    verify(emailActionEventPublisher, times(1))
        .publishEmailActionEvent(emailActionEventArgumentCaptor.capture());
    EmailActionEvent emailActionEvent = emailActionEventArgumentCaptor.getValue();
    assertThat(emailActionEvent.getSenderId()).isEqualTo(20L);
    assertThat(emailActionEvent.getEmailTemplateId()).isEqualTo(1L);
    assertThat(emailActionEvent.getRelatedTo().getId()).isEqualTo(55L);
    assertThat(emailActionEvent.getRelatedTo().getEntity()).isEqualTo(EmailEntityType.LEAD.getEntityName());
    assertThat(emailActionEvent.getRelatedTo().getEmail()).isEqualTo("abc@primary.com");
    assertThat(emailActionEvent.getRelatedTo().getName()).isEqualTo("new lead");
    assertThat(emailActionEvent.getTenantId()).isEqualTo(10L);
    assertThat(emailActionEvent.getUserId()).isEqualTo(5L);

  }

  @Test
  public void givenEmailActionOnContactEvent_fromRecordOwnerToParticipants_shouldProcessItAndPublishEvent() throws IOException {
    //given
    Object from = getFrom(EmailActionType.RECORD_OWNER.name(), 1L, "record owner", "record.owner@gmail.com");
    List<Participant> participants = List
        .of(new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.USER.getEntityName(), 1L, "test user", "test@user.com"),
            new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.USER.getEntityName(), 2L, "test user", "test@user.com"));

    var contactDetail = new ContactDetail();
    contactDetail.setName("new contact");
    contactDetail.setOwnerId(new IdName(20L, "contact owner"));
    contactDetail.setEmails(
        List.of(new Email(EmailType.PERSONAL, "abc@primary.com", true), new Email(EmailType.OFFICE, "abc@123.com", false)).toArray(Email[]::new));
    contactDetail.setId(55L);
    Metadata metadata = new Metadata(10L, 20L, CONTACT, null, null, EntityAction.CREATED);

    var contactEvent = new ContactEvent(contactDetail, null, metadata);
    EmailAction emailAction = new EmailAction(1L, from, participants, participants, participants);
    User aUser = UserStub.aUser(20L, 5L, true, true, true, true, true)
        .withName("updated contactOwner");

    given(userService.getUserDetails(20L, authService.getAuthenticationToken())).willReturn(Mono.just(aUser));
    //when
    emailActionService.processEmailAction(emailAction, contactEvent);

    //then
    verify(emailActionEventPublisher, times(1))
        .publishEmailActionEvent(emailActionEventArgumentCaptor.capture());
    EmailActionEvent emailActionEvent = emailActionEventArgumentCaptor.getValue();
    assertThat(emailActionEvent.getSenderId()).isEqualTo(20L);
    assertThat(emailActionEvent.getEmailTemplateId()).isEqualTo(1L);
    assertThat(emailActionEvent.getRelatedTo().getId()).isEqualTo(55L);
    assertThat(emailActionEvent.getRelatedTo().getEntity()).isEqualTo(EmailEntityType.CONTACT.getEntityName());
    assertThat(emailActionEvent.getRelatedTo().getEmail()).isEqualTo("abc@primary.com");
    assertThat(emailActionEvent.getRelatedTo().getName()).isEqualTo("new contact");
    assertThat(emailActionEvent.getTenantId()).isEqualTo(10L);
    assertThat(emailActionEvent.getUserId()).isEqualTo(20L);

  }

  @Test
  public void givenEmailActionOnDealEvent_fromRecordOwnerToParticipants_shouldProcessItAndPublishEvent() throws IOException {
    //given
    Object from = getFrom(EmailActionType.RECORD_OWNER.name(), 1L, "record owner", "record.owner@gmail.com");
    List<Participant> participants = List
        .of(new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.USER.getEntityName(), 1L, "test user", "test@user.com"),
            new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.USER.getEntityName(), 2L, "test user", "test@user.com"));

    var dealDetail = new DealDetail();
    dealDetail.setOwnedBy(new IdName(20L, "deal owner"));
    dealDetail.setName("new deal");
    dealDetail.setId(55L);
    Metadata metadata = new Metadata(10L, 20L, DEAL, null, null, EntityAction.CREATED);

    var dealEvent = new DealEvent(dealDetail, null, metadata);
    EmailAction emailAction = new EmailAction(1L, from, participants, participants, participants);
    User aUser = UserStub.aUser(20L, 5L, true, true, true, true, true)
        .withName("updated dealOwner");

    given(userService.getUserDetails(20L, authService.getAuthenticationToken())).willReturn(Mono.just(aUser));
    //when
    emailActionService.processEmailAction(emailAction, dealEvent);

    //then
    verify(emailActionEventPublisher, times(1))
        .publishEmailActionEvent(emailActionEventArgumentCaptor.capture());
    EmailActionEvent emailActionEvent = emailActionEventArgumentCaptor.getValue();
    assertThat(emailActionEvent.getSenderId()).isEqualTo(20L);
    assertThat(emailActionEvent.getEmailTemplateId()).isEqualTo(1L);
    assertThat(emailActionEvent.getRelatedTo().getId()).isEqualTo(55L);
    assertThat(emailActionEvent.getRelatedTo().getEntity()).isEqualTo(EmailEntityType.DEAL.getEntityName());
    assertThat(emailActionEvent.getRelatedTo().getEmail()).isNull();
    assertThat(emailActionEvent.getRelatedTo().getName()).isEqualTo("new deal");
    assertThat(emailActionEvent.getTenantId()).isEqualTo(10L);
    assertThat(emailActionEvent.getUserId()).isEqualTo(20L);
  }

  @Test
  public void givenEmailActionOnLeadEvent_fromRecordCreatedByToParticipants_shouldProcessItAndPublishEvent() throws IOException {
    //given
    Object from = getFrom(EmailActionType.RECORD_CREATED_BY.name(), 1L, "record createdBy", "record.createdBy@gmail.com");
    List<Participant> participants = List
        .of(new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.USER.getEntityName(), 1L, "test user", "test@user.com"),
            new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.USER.getEntityName(), 2L, "test user", "test@user.com"));

    var lead = new LeadDetail();
    lead.setName("new lead");
    lead.setCreatedBy(new IdName(20L, "lead createdBy"));
    lead.setOwnerId(new IdName(20L, "lead owner"));
    lead.setEmails(
        List.of(new Email(EmailType.PERSONAL, "abc@primary.com", true), new Email(EmailType.OFFICE, "abc@123.com", false)).toArray(Email[]::new));
    lead.setId(55L);
    Metadata metadata = new Metadata(10L, 5L, LEAD, null, null, EntityAction.CREATED);

    var leadEvent = new LeadEvent(lead, null, metadata);
    EmailAction emailAction = new EmailAction(1L, from, participants, participants, participants);
    User aUser = UserStub.aUser(20L, 5L, true, true, true, true, true)
        .withName("Updated leadCreatedBy");

    given(userService.getUserDetails(20L, authService.getAuthenticationToken())).willReturn(Mono.just(aUser));
    //when
    emailActionService.processEmailAction(emailAction, leadEvent);

    //then
    verify(emailActionEventPublisher, times(1))
        .publishEmailActionEvent(emailActionEventArgumentCaptor.capture());
    EmailActionEvent emailActionEvent = emailActionEventArgumentCaptor.getValue();
    assertThat(emailActionEvent.getSenderId()).isEqualTo(20L);
    assertThat(emailActionEvent.getEmailTemplateId()).isEqualTo(1L);
    assertThat(emailActionEvent.getRelatedTo().getId()).isEqualTo(55L);
    assertThat(emailActionEvent.getRelatedTo().getEntity()).isEqualTo(EmailEntityType.LEAD.getEntityName());
    assertThat(emailActionEvent.getRelatedTo().getEmail()).isEqualTo("abc@primary.com");
    assertThat(emailActionEvent.getRelatedTo().getName()).isEqualTo("new lead");
    assertThat(emailActionEvent.getTenantId()).isEqualTo(10L);
    assertThat(emailActionEvent.getUserId()).isEqualTo(5L);
  }

  @Test
  public void givenEmailActionOnContactEvent_fromRecordCreatedByToParticipants_shouldProcessItAndPublishEvent() throws IOException {
    //given
    Object from = getFrom(EmailActionType.RECORD_CREATED_BY.name(), 1L, "record createdBy", "record.createdBy@gmail.com");
    List<Participant> participants = List
        .of(new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.USER.getEntityName(), 1L, "test user", "test@user.com"),
            new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.USER.getEntityName(), 2L, "test user", "test@user.com"));

    var contactDetail = new ContactDetail();
    contactDetail.setName("new contact");
    contactDetail.setCreatedBy(new IdName(20L, "contact createdBy"));
    contactDetail.setOwnerId(new IdName(20L, "contact owner"));
    contactDetail.setEmails(
        List.of(new Email(EmailType.PERSONAL, "abc@primary.com", true), new Email(EmailType.OFFICE, "abc@123.com", false)).toArray(Email[]::new));
    contactDetail.setId(55L);
    Metadata metadata = new Metadata(10L, 20L, CONTACT, null, null, EntityAction.CREATED);

    var contactEvent = new ContactEvent(contactDetail, null, metadata);
    EmailAction emailAction = new EmailAction(1L, from, participants, participants, participants);
    User aUser = UserStub.aUser(20L, 5L, true, true, true, true, true)
        .withName("updated contactCreatedBy");

    given(userService.getUserDetails(20L, authService.getAuthenticationToken())).willReturn(Mono.just(aUser));
    //when
    emailActionService.processEmailAction(emailAction, contactEvent);

    //then
    verify(emailActionEventPublisher, times(1))
        .publishEmailActionEvent(emailActionEventArgumentCaptor.capture());
    EmailActionEvent emailActionEvent = emailActionEventArgumentCaptor.getValue();
    assertThat(emailActionEvent.getSenderId()).isEqualTo(20L);
    assertThat(emailActionEvent.getEmailTemplateId()).isEqualTo(1L);
    assertThat(emailActionEvent.getRelatedTo().getId()).isEqualTo(55L);
    assertThat(emailActionEvent.getRelatedTo().getEntity()).isEqualTo(EmailEntityType.CONTACT.getEntityName());
    assertThat(emailActionEvent.getRelatedTo().getEmail()).isEqualTo("abc@primary.com");
    assertThat(emailActionEvent.getRelatedTo().getName()).isEqualTo("new contact");
    assertThat(emailActionEvent.getTenantId()).isEqualTo(10L);
    assertThat(emailActionEvent.getUserId()).isEqualTo(20L);
  }

  @Test
  public void givenEmailActionOnDealEvent_fromRecordCreatedByToParticipants_shouldProcessItAndPublishEvent() throws IOException {
    //given
    Object from = getFrom(EmailActionType.RECORD_CREATED_BY.name(), 1L, "record createdBy", "record.createdBy@gmail.com");
    List<Participant> participants = List
        .of(new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.USER.getEntityName(), 1L, "test user", "test@user.com"),
            new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.USER.getEntityName(), 2L, "test user", "test@user.com"));

    var dealDetail = new DealDetail();
    dealDetail.setName("new deal");
    dealDetail.setCreatedBy(new IdName(20L, "deal createdBy"));
    dealDetail.setOwnedBy(new IdName(20L, "deal owner"));
    dealDetail.setId(55L);
    Metadata metadata = new Metadata(10L, 20L, DEAL, null, null, EntityAction.CREATED);

    var dealEvent = new DealEvent(dealDetail, null, metadata);
    EmailAction emailAction = new EmailAction(1L, from, participants, participants, participants);
    User aUser = UserStub.aUser(20L, 5L, true, true, true, true, true)
        .withName("updated dealCreatedBy");

    given(userService.getUserDetails(20L, authService.getAuthenticationToken())).willReturn(Mono.just(aUser));
    //when
    emailActionService.processEmailAction(emailAction, dealEvent);

    //then
    verify(emailActionEventPublisher, times(1))
        .publishEmailActionEvent(emailActionEventArgumentCaptor.capture());
    EmailActionEvent emailActionEvent = emailActionEventArgumentCaptor.getValue();
    assertThat(emailActionEvent.getSenderId()).isEqualTo(20L);
    assertThat(emailActionEvent.getEmailTemplateId()).isEqualTo(1L);
    assertThat(emailActionEvent.getRelatedTo().getId()).isEqualTo(55L);
    assertThat(emailActionEvent.getRelatedTo().getEntity()).isEqualTo(EmailEntityType.DEAL.getEntityName());
    assertThat(emailActionEvent.getRelatedTo().getEmail()).isNull();
    assertThat(emailActionEvent.getRelatedTo().getName()).isEqualTo("new deal");
    assertThat(emailActionEvent.getTenantId()).isEqualTo(10L);
    assertThat(emailActionEvent.getUserId()).isEqualTo(20L);
  }

  @Test
  public void givenEmailActionOnLeadEvent_fromRecordUpdatedByToParticipants_shouldProcessItAndPublishEvent() throws IOException {
    //given
    Object from = getFrom(EmailActionType.RECORD_UPDATED_BY.name(), 1L, "record updatedBy", "record.updatedBy@gmail.com");
    List<Participant> participants = List
        .of(new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.USER.getEntityName(), 1L, "test user", "test@user.com"),
            new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.USER.getEntityName(), 2L, "test user", "test@user.com"));

    var lead = new LeadDetail();
    lead.setName("new lead");
    lead.setUpdatedBy(new IdName(20L, "lead updatedBy"));
    lead.setOwnerId(new IdName(20L, "lead owner"));
    lead.setEmails(
        List.of(new Email(EmailType.PERSONAL, "abc@primary.com", true), new Email(EmailType.OFFICE, "abc@123.com", false)).toArray(Email[]::new));
    lead.setId(55L);
    Metadata metadata = new Metadata(10L, 5L, LEAD, null, null, EntityAction.CREATED);

    var leadEvent = new LeadEvent(lead, null, metadata);
    EmailAction emailAction = new EmailAction(1L, from, participants, participants, participants);
    User aUser = UserStub.aUser(20L, 5L, true, true, true, true, true)
        .withName("Updated leadUpdatedBy");

    given(userService.getUserDetails(20L, authService.getAuthenticationToken())).willReturn(Mono.just(aUser));
    //when
    emailActionService.processEmailAction(emailAction, leadEvent);

    //then
    verify(emailActionEventPublisher, times(1))
        .publishEmailActionEvent(emailActionEventArgumentCaptor.capture());
    EmailActionEvent emailActionEvent = emailActionEventArgumentCaptor.getValue();
    assertThat(emailActionEvent.getSenderId()).isEqualTo(20L);
    assertThat(emailActionEvent.getEmailTemplateId()).isEqualTo(1L);
    assertThat(emailActionEvent.getRelatedTo().getId()).isEqualTo(55L);
    assertThat(emailActionEvent.getRelatedTo().getEntity()).isEqualTo(EmailEntityType.LEAD.getEntityName());
    assertThat(emailActionEvent.getRelatedTo().getEmail()).isEqualTo("abc@primary.com");
    assertThat(emailActionEvent.getRelatedTo().getName()).isEqualTo("new lead");
    assertThat(emailActionEvent.getTenantId()).isEqualTo(10L);
    assertThat(emailActionEvent.getUserId()).isEqualTo(5L);
  }

  @Test
  public void givenEmailActionOnContactEvent_fromRecordUpdatedByToParticipants_shouldProcessItAndPublishEvent() throws IOException {
    //given
    Object from = getFrom(EmailActionType.RECORD_UPDATED_BY.name(), 1L, "record updatedBy", "record.updatedBy@gmail.com");
    List<Participant> participants = List
        .of(new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.USER.getEntityName(), 1L, "test user", "test@user.com"),
            new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.USER.getEntityName(), 2L, "test user", "test@user.com"));

    var contactDetail = new ContactDetail();
    contactDetail.setName("new contact");
    contactDetail.setUpdatedBy(new IdName(20L, "contact updatedBy"));
    contactDetail.setOwnerId(new IdName(20L, "contact owner"));
    contactDetail.setEmails(
        List.of(new Email(EmailType.PERSONAL, "abc@primary.com", true), new Email(EmailType.OFFICE, "abc@123.com", false)).toArray(Email[]::new));
    contactDetail.setId(55L);
    Metadata metadata = new Metadata(10L, 20L, CONTACT, null, null, EntityAction.CREATED);

    var contactEvent = new ContactEvent(contactDetail, null, metadata);
    EmailAction emailAction = new EmailAction(1L, from, participants, participants, participants);
    User aUser = UserStub.aUser(20L, 5L, true, true, true, true, true)
        .withName("updated contactUpdatedBy");

    given(userService.getUserDetails(20L, authService.getAuthenticationToken())).willReturn(Mono.just(aUser));
    //when
    emailActionService.processEmailAction(emailAction, contactEvent);

    //then
    verify(emailActionEventPublisher, times(1))
        .publishEmailActionEvent(emailActionEventArgumentCaptor.capture());
    EmailActionEvent emailActionEvent = emailActionEventArgumentCaptor.getValue();
    assertThat(emailActionEvent.getSenderId()).isEqualTo(20L);
    assertThat(emailActionEvent.getEmailTemplateId()).isEqualTo(1L);
    assertThat(emailActionEvent.getRelatedTo().getId()).isEqualTo(55L);
    assertThat(emailActionEvent.getRelatedTo().getEntity()).isEqualTo(EmailEntityType.CONTACT.getEntityName());
    assertThat(emailActionEvent.getRelatedTo().getEmail()).isEqualTo("abc@primary.com");
    assertThat(emailActionEvent.getRelatedTo().getName()).isEqualTo("new contact");
    assertThat(emailActionEvent.getTenantId()).isEqualTo(10L);
    assertThat(emailActionEvent.getUserId()).isEqualTo(20L);
  }

  @Test
  public void givenEmailActionOnDealEvent_fromRecordUpdatedByToParticipants_shouldProcessItAndPublishEvent() throws IOException {
    //given
    Object from = getFrom(EmailActionType.RECORD_UPDATED_BY.name(), 1L, "record updatedBy", "record.updatedBy@gmail.com");
    List<Participant> participants = List
        .of(new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.USER.getEntityName(), 1L, "test user", "test@user.com"),
            new Participant(EmailActionType.RECORD_OWNER, EmailEntityType.USER.getEntityName(), 2L, "test user", "test@user.com"));

    var dealDetail = new DealDetail();
    dealDetail.setName("new deal");
    dealDetail.setUpdatedBy(new IdName(20L, "deal updatedBy"));
    dealDetail.setOwnedBy(new IdName(20L, "deal owner"));
    dealDetail.setId(55L);
    Metadata metadata = new Metadata(10L, 20L, DEAL, null, null, EntityAction.CREATED);

    var dealEvent = new DealEvent(dealDetail, null, metadata);
    EmailAction emailAction = new EmailAction(1L, from, participants, participants, participants);
    User aUser = UserStub.aUser(20L, 5L, true, true, true, true, true)
        .withName("updated dealUpdatedBy");

    given(userService.getUserDetails(20L, authService.getAuthenticationToken())).willReturn(Mono.just(aUser));
    //when
    emailActionService.processEmailAction(emailAction, dealEvent);

    //then
    verify(emailActionEventPublisher, times(1))
        .publishEmailActionEvent(emailActionEventArgumentCaptor.capture());
    EmailActionEvent emailActionEvent = emailActionEventArgumentCaptor.getValue();
    assertThat(emailActionEvent.getSenderId()).isEqualTo(20L);
    assertThat(emailActionEvent.getEmailTemplateId()).isEqualTo(1L);
    assertThat(emailActionEvent.getRelatedTo().getId()).isEqualTo(55L);
    assertThat(emailActionEvent.getRelatedTo().getEntity()).isEqualTo(EmailEntityType.DEAL.getEntityName());
    assertThat(emailActionEvent.getRelatedTo().getEmail()).isNull();
    assertThat(emailActionEvent.getRelatedTo().getName()).isEqualTo("new deal");
    assertThat(emailActionEvent.getTenantId()).isEqualTo(10L);
    assertThat(emailActionEvent.getUserId()).isEqualTo(20L);
  }

  @Test
  public void givenEmailActionOnEntityEvent_fromWorkflowCreatorToWorkflowCreator_shouldProcessItAndPublishEvent() throws IOException {
    //given
    Object from = getFrom(EmailActionType.WORKFLOW_CREATOR.name(), 1L, "workflow creator", "workflow.creator@gmail.com");
    List<Participant> participants = List
        .of(new Participant(EmailActionType.WORKFLOW_CREATOR, EmailEntityType.USER.getEntityName(), 1L, "test user", "test@user.com"),
            new Participant(EmailActionType.WORKFLOW_CREATOR, EmailEntityType.USER.getEntityName(), 2L, "test user", "test@user.com"));

    var lead = new LeadDetail();
    lead.setName("new lead");
    lead.setUpdatedBy(new IdName(20L, "lead updatedBy"));
    lead.setOwnerId(new IdName(20L, "lead owner"));
    lead.setEmails(
        List.of(new Email(EmailType.PERSONAL, "abc@primary.com", true), new Email(EmailType.OFFICE, "abc@123.com", false)).toArray(Email[]::new));
    lead.setId(55L);
    Metadata metadata = new Metadata(10L, 5L, LEAD, null, null, EntityAction.CREATED);

    var leadEvent = new LeadEvent(lead, null, metadata);
    EmailAction emailAction = new EmailAction(1L, from, participants, participants, participants);
    User aUser = UserStub.aUser(20L, 5L, true, true, true, true, true)
        .withName("Updated lead");
    Workflow workflow = new Workflow();
    workflow.setCreatedBy(aUser);
    emailAction.setWorkflow(workflow);
    given(userService.getUserDetails(20L, authService.getAuthenticationToken())).willReturn(Mono.just(aUser));
    //when
    emailActionService.processEmailAction(emailAction, leadEvent);

    //then
    verify(emailActionEventPublisher, times(1))
        .publishEmailActionEvent(emailActionEventArgumentCaptor.capture());
    EmailActionEvent emailActionEvent = emailActionEventArgumentCaptor.getValue();
    assertThat(emailActionEvent.getSenderId()).isEqualTo(20L);
    assertThat(emailActionEvent.getEmailTemplateId()).isEqualTo(1L);
    assertThat(emailActionEvent.getRelatedTo().getId()).isEqualTo(55L);
    assertThat(emailActionEvent.getRelatedTo().getEntity()).isEqualTo(EmailEntityType.LEAD.getEntityName());
    assertThat(emailActionEvent.getRelatedTo().getEmail()).isEqualTo("abc@primary.com");
    assertThat(emailActionEvent.getRelatedTo().getName()).isEqualTo("new lead");
    assertThat(emailActionEvent.getTenantId()).isEqualTo(10L);
    assertThat(emailActionEvent.getUserId()).isEqualTo(5L);
    assertThat(emailActionEvent.getTo()).allMatch(
        participant -> participant.getId() == 20L && participant.getName().equals("Updated lead") && participant.getEntity()
            .equals(EmailEntityType.USER.getEntityName()));
    assertThat(emailActionEvent.getCc()).allMatch(
        participant -> participant.getId() == 20L && participant.getName().equals("Updated lead") && participant.getEntity()
            .equals(EmailEntityType.USER.getEntityName()));
    assertThat(emailActionEvent.getBcc()).allMatch(
        participant -> participant.getId() == 20L && participant.getName().equals("Updated lead") && participant.getEntity()
            .equals(EmailEntityType.USER.getEntityName()));
  }

  @Test
  public void givenEmailActionOnEntityEvent_fromWorkflowUpdaterToWorkflowUpdater_shouldProcessItAndPublishEvent() throws IOException {
    //given
    Object from = getFrom(EmailActionType.WORKFLOW_UPDATER.name(), 1L, "workflow updater", "workflow.updater@gmail.com");
    List<Participant> participants = List
        .of(new Participant(EmailActionType.WORKFLOW_UPDATER, EmailEntityType.USER.getEntityName(), 1L, "test user", "test@user.com"),
            new Participant(EmailActionType.WORKFLOW_UPDATER, EmailEntityType.USER.getEntityName(), 2L, "test user", "test@user.com"));

    var lead = new LeadDetail();
    lead.setName("new lead");
    lead.setUpdatedBy(new IdName(20L, "lead updatedBy"));
    lead.setOwnerId(new IdName(20L, "lead owner"));
    lead.setEmails(
        List.of(new Email(EmailType.PERSONAL, "abc@primary.com", true), new Email(EmailType.OFFICE, "abc@123.com", false)).toArray(Email[]::new));
    lead.setId(55L);
    Metadata metadata = new Metadata(10L, 5L, LEAD, null, null, EntityAction.CREATED);

    var leadEvent = new LeadEvent(lead, null, metadata);
    EmailAction emailAction = new EmailAction(1L, from, participants, participants, participants);
    User aUser = UserStub.aUser(20L, 5L, true, true, true, true, true)
        .withName("Updated lead");
    Workflow workflow = new Workflow();
    workflow.setUpdatedBy(aUser);
    emailAction.setWorkflow(workflow);
    given(userService.getUserDetails(20L, authService.getAuthenticationToken())).willReturn(Mono.just(aUser));
    //when
    emailActionService.processEmailAction(emailAction, leadEvent);

    //then
    verify(emailActionEventPublisher, times(1))
        .publishEmailActionEvent(emailActionEventArgumentCaptor.capture());
    EmailActionEvent emailActionEvent = emailActionEventArgumentCaptor.getValue();
    assertThat(emailActionEvent.getSenderId()).isEqualTo(20L);
    assertThat(emailActionEvent.getEmailTemplateId()).isEqualTo(1L);
    assertThat(emailActionEvent.getRelatedTo().getId()).isEqualTo(55L);
    assertThat(emailActionEvent.getRelatedTo().getEntity()).isEqualTo(EmailEntityType.LEAD.getEntityName());
    assertThat(emailActionEvent.getRelatedTo().getEmail()).isEqualTo("abc@primary.com");
    assertThat(emailActionEvent.getRelatedTo().getName()).isEqualTo("new lead");
    assertThat(emailActionEvent.getTenantId()).isEqualTo(10L);
    assertThat(emailActionEvent.getUserId()).isEqualTo(5L);
    assertThat(emailActionEvent.getTo()).allMatch(
        participant -> participant.getId() == 20L && participant.getName().equals("Updated lead") && participant.getEntity()
            .equals(EmailEntityType.USER.getEntityName()));
    assertThat(emailActionEvent.getCc()).allMatch(
        participant -> participant.getId() == 20L && participant.getName().equals("Updated lead") && participant.getEntity()
            .equals(EmailEntityType.USER.getEntityName()));
    assertThat(emailActionEvent.getBcc()).allMatch(
        participant -> participant.getId() == 20L && participant.getName().equals("Updated lead") && participant.getEntity()
            .equals(EmailEntityType.USER.getEntityName()));
  }

  @Test
  public void givenEmailActionOnEntityEvent_fromUserToUser_shouldProcessItAndPublishEvent() throws IOException {
    //given
    Object from = getFrom(EmailActionType.USER.name(), 20L, "workflow creator", "workflow.creator@gmail.com");
    List<Participant> participants = List
        .of(new Participant(EmailActionType.USER, EmailEntityType.USER.getEntityName(), 20L, "test user", "test@user.com"),
            new Participant(EmailActionType.USER, EmailEntityType.USER.getEntityName(), 20L, "test user", "test@user.com"));

    var lead = new LeadDetail();
    lead.setName("new lead");
    lead.setUpdatedBy(new IdName(20L, "lead updatedBy"));
    lead.setOwnerId(new IdName(20L, "lead owner"));
    lead.setEmails(
        List.of(new Email(EmailType.PERSONAL, "abc@primary.com", true), new Email(EmailType.OFFICE, "abc@123.com", false)).toArray(Email[]::new));
    lead.setId(55L);
    Metadata metadata = new Metadata(10L, 5L, LEAD, null, null, EntityAction.CREATED);

    var leadEvent = new LeadEvent(lead, null, metadata);
    EmailAction emailAction = new EmailAction(1L, from, participants, participants, participants);
    User aUser = UserStub.aUser(20L, 5L, true, true, true, true, true)
        .withName("Updated lead");

    given(userService.getUserDetails(20L, authService.getAuthenticationToken())).willReturn(Mono.just(aUser));
    //when
    emailActionService.processEmailAction(emailAction, leadEvent);

    //then
    verify(emailActionEventPublisher, times(1))
        .publishEmailActionEvent(emailActionEventArgumentCaptor.capture());
    EmailActionEvent emailActionEvent = emailActionEventArgumentCaptor.getValue();
    assertThat(emailActionEvent.getSenderId()).isEqualTo(20L);
    assertThat(emailActionEvent.getEmailTemplateId()).isEqualTo(1L);
    assertThat(emailActionEvent.getRelatedTo().getId()).isEqualTo(55L);
    assertThat(emailActionEvent.getRelatedTo().getEntity()).isEqualTo(EmailEntityType.LEAD.getEntityName());
    assertThat(emailActionEvent.getRelatedTo().getEmail()).isEqualTo("abc@primary.com");
    assertThat(emailActionEvent.getRelatedTo().getName()).isEqualTo("new lead");
    assertThat(emailActionEvent.getTenantId()).isEqualTo(10L);
    assertThat(emailActionEvent.getUserId()).isEqualTo(5L);
    assertThat(emailActionEvent.getTo()).allMatch(
        participant -> participant.getId() == 20L && participant.getName().equals("Updated lead") && participant.getEntity()
            .equals(EmailEntityType.USER.getEntityName()));
    assertThat(emailActionEvent.getCc()).allMatch(
        participant -> participant.getId() == 20L && participant.getName().equals("Updated lead") && participant.getEntity()
            .equals(EmailEntityType.USER.getEntityName()));
    assertThat(emailActionEvent.getBcc()).allMatch(
        participant -> participant.getId() == 20L && participant.getName().equals("Updated lead") && participant.getEntity()
            .equals(EmailEntityType.USER.getEntityName()));
  }

  @Test
  public void givenEmailActionOnLeadEvent_fromWorkflowCreatorToRecordPrimaryEmail_shouldProcessItAndPublishEvent() throws IOException {
    //given
    Object from = getFrom(EmailActionType.WORKFLOW_CREATOR.name(), 1L, "workflow creator", "workflow.creator@gmail.com");
    List<Participant> participants = List
        .of(new Participant(EmailActionType.RECORD_PRIMARY_EMAIL, EmailEntityType.LEAD.getEntityName(), 20L, "test user", "test@user.com"),
            new Participant(EmailActionType.RECORD_PRIMARY_EMAIL, EmailEntityType.LEAD.getEntityName(), 20L, "test user", "test@user.com"));

    var lead = new LeadDetail();
    lead.setName("new lead");
    lead.setUpdatedBy(new IdName(20L, "lead updatedBy"));
    lead.setOwnerId(new IdName(20L, "lead owner"));
    lead.setEmails(
        List.of(new Email(EmailType.PERSONAL, "abc@primary.com", true), new Email(EmailType.OFFICE, "abc@123.com", false)).toArray(Email[]::new));
    lead.setId(55L);
    Metadata metadata = new Metadata(10L, 5L, LEAD, null, null, EntityAction.CREATED);

    var leadEvent = new LeadEvent(lead, null, metadata);
    EmailAction emailAction = new EmailAction(1L, from, participants, participants, participants);
    User aUser = UserStub.aUser(20L, 5L, true, true, true, true, true)
        .withName("Updated lead");
    Workflow workflow = new Workflow();
    workflow.setCreatedBy(aUser);
    emailAction.setWorkflow(workflow);
    given(userService.getUserDetails(20L, authService.getAuthenticationToken())).willReturn(Mono.just(aUser));
    //when
    emailActionService.processEmailAction(emailAction, leadEvent);

    //then
    verify(emailActionEventPublisher, times(1))
        .publishEmailActionEvent(emailActionEventArgumentCaptor.capture());
    EmailActionEvent emailActionEvent = emailActionEventArgumentCaptor.getValue();
    assertThat(emailActionEvent.getSenderId()).isEqualTo(20L);
    assertThat(emailActionEvent.getEmailTemplateId()).isEqualTo(1L);
    assertThat(emailActionEvent.getRelatedTo().getId()).isEqualTo(55L);
    assertThat(emailActionEvent.getRelatedTo().getEntity()).isEqualTo(EmailEntityType.LEAD.getEntityName());
    assertThat(emailActionEvent.getRelatedTo().getEmail()).isEqualTo("abc@primary.com");
    assertThat(emailActionEvent.getRelatedTo().getName()).isEqualTo("new lead");
    assertThat(emailActionEvent.getTenantId()).isEqualTo(10L);
    assertThat(emailActionEvent.getUserId()).isEqualTo(5L);
    assertThat(emailActionEvent.getTo()).allMatch(
        participant -> participant.getId() == 55L && participant.getName().equals("new lead") && participant.getEntity()
            .equals(EmailEntityType.LEAD.getEntityName()) && participant.getEmail().equals("abc@primary.com"));
    assertThat(emailActionEvent.getCc()).allMatch(
        participant -> participant.getId() == 55L && participant.getName().equals("new lead") && participant.getEntity()
            .equals(EmailEntityType.LEAD.getEntityName()) && participant.getEmail().equals("abc@primary.com"));
    assertThat(emailActionEvent.getBcc()).allMatch(
        participant -> participant.getId() == 55L && participant.getName().equals("new lead") && participant.getEntity()
            .equals(EmailEntityType.LEAD.getEntityName()) && participant.getEmail().equals("abc@primary.com"));
  }

  @Test
  public void givenEmailActionOnEntityEvent_withoutNoEmails_shouldNotPublishEvent() throws IOException {
    //given
    Object from = getFrom(EmailActionType.WORKFLOW_CREATOR.name(), 1L, "workflow creator", "workflow.creator@gmail.com");
    List<Participant> participants = List
        .of(new Participant(EmailActionType.RECORD_PRIMARY_EMAIL, EmailEntityType.LEAD.getEntityName(), 20L, "test user", "test@user.com"),
            new Participant(EmailActionType.RECORD_PRIMARY_EMAIL, EmailEntityType.LEAD.getEntityName(), 20L, "test user", "test@user.com"));

    var lead = new LeadDetail();
    lead.setName("new lead");
    lead.setUpdatedBy(new IdName(20L, "lead updatedBy"));
    lead.setOwnerId(new IdName(20L, "lead owner"));
    lead.setId(55L);
    Metadata metadata = new Metadata(10L, 5L, LEAD, null, null, EntityAction.CREATED);

    var leadEvent = new LeadEvent(lead, null, metadata);
    EmailAction emailAction = new EmailAction(1L, from, participants, participants, participants);
    User aUser = UserStub.aUser(20L, 5L, true, true, true, true, true)
        .withName("Updated lead");
    Workflow workflow = new Workflow();
    workflow.setCreatedBy(aUser);
    emailAction.setWorkflow(workflow);
    given(userService.getUserDetails(20L, authService.getAuthenticationToken())).willReturn(Mono.just(aUser));
    //when
    emailActionService.processEmailAction(emailAction, leadEvent);

    //then
    verifyNoMoreInteractions(emailActionEventPublisher);
  }


  @Test
  public void givenEmailActionOnContactEvent_fromWorkflowCreatorToRecordPrimaryEmail_shouldProcessItAndPublishEvent() throws IOException {
    //given
    Object from = getFrom(EmailActionType.WORKFLOW_CREATOR.name(), 1L, "workflow creator", "workflow.creator@gmail.com");
    List<Participant> participants = List
        .of(new Participant(EmailActionType.RECORD_PRIMARY_EMAIL, EmailEntityType.CONTACT.getEntityName(), 20L, "test user", "test@user.com"),
            new Participant(EmailActionType.RECORD_PRIMARY_EMAIL, EmailEntityType.CONTACT.getEntityName(), 20L, "test user", "test@user.com"));

    var contactDetail = new ContactDetail();
    contactDetail.setName("new contact");
    contactDetail.setUpdatedBy(new IdName(20L, "lead updatedBy"));
    contactDetail.setOwnerId(new IdName(20L, "lead owner"));
    contactDetail.setEmails(
        List.of(new Email(EmailType.PERSONAL, "abc@primary.com", true), new Email(EmailType.OFFICE, "abc@123.com", false)).toArray(Email[]::new));
    contactDetail.setId(55L);
    Metadata metadata = new Metadata(10L, 5L, CONTACT, null, null, EntityAction.CREATED);

    var contactEvent = new ContactEvent(contactDetail, null, metadata);
    EmailAction emailAction = new EmailAction(1L, from, participants, participants, participants);
    User aUser = UserStub.aUser(20L, 5L, true, true, true, true, true)
        .withName("Updated contact");
    Workflow workflow = new Workflow();
    workflow.setCreatedBy(aUser);
    emailAction.setWorkflow(workflow);
    given(userService.getUserDetails(20L, authService.getAuthenticationToken())).willReturn(Mono.just(aUser));
    //when
    emailActionService.processEmailAction(emailAction, contactEvent);

    //then
    verify(emailActionEventPublisher, times(1))
        .publishEmailActionEvent(emailActionEventArgumentCaptor.capture());
    EmailActionEvent emailActionEvent = emailActionEventArgumentCaptor.getValue();
    assertThat(emailActionEvent.getSenderId()).isEqualTo(20L);
    assertThat(emailActionEvent.getEmailTemplateId()).isEqualTo(1L);
    assertThat(emailActionEvent.getRelatedTo().getId()).isEqualTo(55L);
    assertThat(emailActionEvent.getRelatedTo().getEntity()).isEqualTo(EmailEntityType.CONTACT.getEntityName());
    assertThat(emailActionEvent.getRelatedTo().getEmail()).isEqualTo("abc@primary.com");
    assertThat(emailActionEvent.getRelatedTo().getName()).isEqualTo("new contact");
    assertThat(emailActionEvent.getTenantId()).isEqualTo(10L);
    assertThat(emailActionEvent.getUserId()).isEqualTo(5L);
    assertThat(emailActionEvent.getTo()).allMatch(
        participant -> participant.getId() == 55L && participant.getName().equals("new contact") && participant.getEntity()
            .equals(EmailEntityType.CONTACT.getEntityName()) && participant.getEmail().equals("abc@primary.com"));
    assertThat(emailActionEvent.getCc()).allMatch(
        participant -> participant.getId() == 55L && participant.getName().equals("new contact") && participant.getEntity()
            .equals(EmailEntityType.CONTACT.getEntityName()) && participant.getEmail().equals("abc@primary.com"));
    assertThat(emailActionEvent.getBcc()).allMatch(
        participant -> participant.getId() == 55L && participant.getName().equals("new contact") && participant.getEntity()
            .equals(EmailEntityType.CONTACT.getEntityName()) && participant.getEmail().equals("abc@primary.com"));
  }

  @Test
  public void givenEmailActionOnDealEvent_fromWorkflowCreatorToAssociatedContacts_shouldProcessItAndPublishEvent() throws IOException {
    //given
    Object from = getFrom(EmailActionType.WORKFLOW_CREATOR.name(), 1L, "workflow creator", "workflow.creator@gmail.com");
    List<Participant> participants = List
        .of(new Participant(EmailActionType.ALL_ASSOCIATED_CONTACTS, EmailEntityType.DEAL.getEntityName(), 20L, "test user", "test@user.com"),
            new Participant(EmailActionType.ALL_ASSOCIATED_CONTACTS, EmailEntityType.DEAL.getEntityName(), 20L, "test user", "test@user.com"));

    var dealDetail = new DealDetail();
    dealDetail.setName("new deal");
    dealDetail.setUpdatedBy(new IdName(20L, "deal updatedBy"));
    dealDetail.setOwnedBy(new IdName(20L, "deal owner"));
    dealDetail.setAssociatedContacts(List.of(new IdName(22L, "new name"), new IdName(22L, "new name")));
    dealDetail.setId(55L);
    Metadata metadata = new Metadata(10L, 5L, DEAL, null, null, EntityAction.CREATED);

    var dealEvent = new DealEvent(dealDetail, null, metadata);
    EmailAction emailAction = new EmailAction(1L, from, participants, participants, participants);
    User aUser = UserStub.aUser(20L, 5L, true, true, true, true, true)
        .withName("Updated deal");
    Workflow workflow = new Workflow();
    workflow.setCreatedBy(aUser);
    emailAction.setWorkflow(workflow);

    Email[] emails = List.of(new Email(EmailType.PERSONAL, "abc@primary.com", true), new Email(EmailType.OFFICE, "abc@123.com", false))
        .toArray(Email[]::new);
    given(contactService.getContactById(22L, authService.getAuthenticationToken())).willReturn(ContactResponse.withEmails(emails));
    given(userService.getUserDetails(20L, authService.getAuthenticationToken())).willReturn(Mono.just(aUser));
    //when
    emailActionService.processEmailAction(emailAction, dealEvent);

    //then
    verify(emailActionEventPublisher, times(1))
        .publishEmailActionEvent(emailActionEventArgumentCaptor.capture());
    EmailActionEvent emailActionEvent = emailActionEventArgumentCaptor.getValue();
    assertThat(emailActionEvent.getSenderId()).isEqualTo(20L);
    assertThat(emailActionEvent.getEmailTemplateId()).isEqualTo(1L);
    assertThat(emailActionEvent.getRelatedTo().getId()).isEqualTo(55L);
    assertThat(emailActionEvent.getRelatedTo().getEntity()).isEqualTo(EmailEntityType.DEAL.getEntityName());
    assertThat(emailActionEvent.getRelatedTo().getEmail()).isNull();
    assertThat(emailActionEvent.getRelatedTo().getName()).isEqualTo("new deal");
    assertThat(emailActionEvent.getTenantId()).isEqualTo(10L);
    assertThat(emailActionEvent.getUserId()).isEqualTo(5L);
    assertThat(emailActionEvent.getTo()).hasSize(4);
    assertThat(emailActionEvent.getCc()).hasSize(4);
    assertThat(emailActionEvent.getBcc()).hasSize(4);
    assertThat(emailActionEvent.getTo()).allMatch(
        participant -> participant.getId() == 55L && participant.getName().equals("new deal") && participant.getEntity()
            .equals(EmailEntityType.DEAL.getEntityName()) && participant.getEmail().equals("abc@primary.com"));
    assertThat(emailActionEvent.getCc()).allMatch(
        participant -> participant.getId() == 55L && participant.getName().equals("new deal") && participant.getEntity()
            .equals(EmailEntityType.DEAL.getEntityName()) && participant.getEmail().equals("abc@primary.com"));
    assertThat(emailActionEvent.getBcc()).allMatch(
        participant -> participant.getId() == 55L && participant.getName().equals("new deal") && participant.getEntity()
            .equals(EmailEntityType.DEAL.getEntityName()) && participant.getEmail().equals("abc@primary.com"));
  }

  @Test
  public void givenEmailActionOnLeadEvent_fromWorkflowCreatorToAllAvailableEmails_shouldProcessItAndPublishEvent() throws IOException {
    //given
    Object from = getFrom(EmailActionType.WORKFLOW_CREATOR.name(), 1L, "workflow creator", "workflow.creator@gmail.com");
    List<Participant> participants = List
        .of(new Participant(EmailActionType.ALL_AVAILABLE_EMAILS, EmailEntityType.LEAD.getEntityName(), 20L, "test user", "test@user.com"),
            new Participant(EmailActionType.ALL_AVAILABLE_EMAILS, EmailEntityType.LEAD.getEntityName(), 20L, "test user", "test@user.com"));

    var lead = new LeadDetail();
    lead.setName("new lead");
    lead.setUpdatedBy(new IdName(20L, "lead updatedBy"));
    lead.setOwnerId(new IdName(20L, "lead owner"));
    lead.setEmails(
        List.of(new Email(EmailType.PERSONAL, "abc@primary.com", true), new Email(EmailType.OFFICE, "abc@123.com", false)).toArray(Email[]::new));
    lead.setId(55L);
    Metadata metadata = new Metadata(10L, 5L, LEAD, null, null, EntityAction.CREATED);

    var leadEvent = new LeadEvent(lead, null, metadata);
    EmailAction emailAction = new EmailAction(1L, from, participants, participants, participants);
    User aUser = UserStub.aUser(20L, 5L, true, true, true, true, true)
        .withName("Updated lead");
    Workflow workflow = new Workflow();
    workflow.setCreatedBy(aUser);
    emailAction.setWorkflow(workflow);
    given(userService.getUserDetails(20L, authService.getAuthenticationToken())).willReturn(Mono.just(aUser));
    //when
    emailActionService.processEmailAction(emailAction, leadEvent);

    //then
    verify(emailActionEventPublisher, times(1))
        .publishEmailActionEvent(emailActionEventArgumentCaptor.capture());
    EmailActionEvent emailActionEvent = emailActionEventArgumentCaptor.getValue();
    assertThat(emailActionEvent.getSenderId()).isEqualTo(20L);
    assertThat(emailActionEvent.getEmailTemplateId()).isEqualTo(1L);
    assertThat(emailActionEvent.getRelatedTo().getId()).isEqualTo(55L);
    assertThat(emailActionEvent.getRelatedTo().getEntity()).isEqualTo(EmailEntityType.LEAD.getEntityName());
    assertThat(emailActionEvent.getRelatedTo().getEmail()).isEqualTo("abc@primary.com");
    assertThat(emailActionEvent.getRelatedTo().getName()).isEqualTo("new lead");
    assertThat(emailActionEvent.getTenantId()).isEqualTo(10L);
    assertThat(emailActionEvent.getUserId()).isEqualTo(5L);
    assertThat(emailActionEvent.getTo()).hasSize(4);
    assertThat(emailActionEvent.getCc()).hasSize(4);
    assertThat(emailActionEvent.getBcc()).hasSize(4);
    com.kylas.sales.workflow.domain.processor.email.Participant firstParticipant = emailActionEvent.getTo().get(0);
    com.kylas.sales.workflow.domain.processor.email.Participant secondParticipant = emailActionEvent.getTo().get(1);
    assertThat(firstParticipant.getId()).isEqualTo(55L);
    assertThat(firstParticipant.getName()).isEqualTo("new lead");
    assertThat(firstParticipant.getEntity()).isEqualTo(EmailEntityType.LEAD.getEntityName());
    assertThat(firstParticipant.getEmail()).isEqualTo("abc@primary.com");
    assertThat(secondParticipant.getId()).isEqualTo(55L);
    assertThat(secondParticipant.getName()).isEqualTo("new lead");
    assertThat(secondParticipant.getEntity()).isEqualTo(EmailEntityType.LEAD.getEntityName());
    assertThat(secondParticipant.getEmail()).isEqualTo("abc@123.com");

  }

  @Test
  public void givenEmailActionOnContactEvent_fromWorkflowCreatorToAllAvailableEmails_shouldProcessItAndPublishEvent() throws IOException {
    //given
    Object from = getFrom(EmailActionType.WORKFLOW_CREATOR.name(), 1L, "workflow creator", "workflow.creator@gmail.com");
    List<Participant> participants = List
        .of(new Participant(EmailActionType.ALL_AVAILABLE_EMAILS, EmailEntityType.CONTACT.getEntityName(), 20L, "test user", "test@user.com"),
            new Participant(EmailActionType.ALL_AVAILABLE_EMAILS, EmailEntityType.CONTACT.getEntityName(), 20L, "test user", "test@user.com"));

    var contactDetail = new ContactDetail();
    contactDetail.setName("new contact");
    contactDetail.setUpdatedBy(new IdName(20L, "contact updatedBy"));
    contactDetail.setOwnerId(new IdName(20L, "contact owner"));
    contactDetail.setEmails(
        List.of(new Email(EmailType.PERSONAL, "abc@primary.com", true), new Email(EmailType.OFFICE, "abc@123.com", false)).toArray(Email[]::new));
    contactDetail.setId(55L);
    Metadata metadata = new Metadata(10L, 5L, CONTACT, null, null, EntityAction.CREATED);

    var contactEvent = new ContactEvent(contactDetail, null, metadata);
    EmailAction emailAction = new EmailAction(1L, from, participants, participants, participants);
    User aUser = UserStub.aUser(20L, 5L, true, true, true, true, true)
        .withName("Updated contact");
    Workflow workflow = new Workflow();
    workflow.setCreatedBy(aUser);
    emailAction.setWorkflow(workflow);
    given(userService.getUserDetails(20L, authService.getAuthenticationToken())).willReturn(Mono.just(aUser));
    //when
    emailActionService.processEmailAction(emailAction, contactEvent);

    //then
    verify(emailActionEventPublisher, times(1))
        .publishEmailActionEvent(emailActionEventArgumentCaptor.capture());
    EmailActionEvent emailActionEvent = emailActionEventArgumentCaptor.getValue();
    assertThat(emailActionEvent.getSenderId()).isEqualTo(20L);
    assertThat(emailActionEvent.getEmailTemplateId()).isEqualTo(1L);
    assertThat(emailActionEvent.getRelatedTo().getId()).isEqualTo(55L);
    assertThat(emailActionEvent.getRelatedTo().getEntity()).isEqualTo(EmailEntityType.CONTACT.getEntityName());
    assertThat(emailActionEvent.getRelatedTo().getEmail()).isEqualTo("abc@primary.com");
    assertThat(emailActionEvent.getRelatedTo().getName()).isEqualTo("new contact");
    assertThat(emailActionEvent.getTenantId()).isEqualTo(10L);
    assertThat(emailActionEvent.getUserId()).isEqualTo(5L);
    assertThat(emailActionEvent.getTo()).hasSize(4);
    assertThat(emailActionEvent.getCc()).hasSize(4);
    assertThat(emailActionEvent.getBcc()).hasSize(4);
    com.kylas.sales.workflow.domain.processor.email.Participant firstParticipant = emailActionEvent.getTo().get(0);
    com.kylas.sales.workflow.domain.processor.email.Participant secondParticipant = emailActionEvent.getTo().get(1);
    assertThat(firstParticipant.getId()).isEqualTo(55L);
    assertThat(firstParticipant.getName()).isEqualTo("new contact");
    assertThat(firstParticipant.getEntity()).isEqualTo(EmailEntityType.CONTACT.getEntityName());
    assertThat(firstParticipant.getEmail()).isEqualTo("abc@primary.com");
    assertThat(secondParticipant.getId()).isEqualTo(55L);
    assertThat(secondParticipant.getName()).isEqualTo("new contact");
    assertThat(secondParticipant.getEntity()).isEqualTo(EmailEntityType.CONTACT.getEntityName());
    assertThat(secondParticipant.getEmail()).isEqualTo("abc@123.com");

  }

  @Test
  public void givenEmailActionOnEntityEvent_withNoAvailableEmails_shouldNotPublishEvent() throws IOException {
    //given
    Object from = getFrom(EmailActionType.WORKFLOW_CREATOR.name(), 1L, "workflow creator", "workflow.creator@gmail.com");
    List<Participant> participants = List
        .of(new Participant(EmailActionType.ALL_AVAILABLE_EMAILS, EmailEntityType.LEAD.getEntityName(), 20L, "test user", "test@user.com"),
            new Participant(EmailActionType.ALL_AVAILABLE_EMAILS, EmailEntityType.LEAD.getEntityName(), 20L, "test user", "test@user.com"));

    var lead = new LeadDetail();
    lead.setName("new lead");
    lead.setUpdatedBy(new IdName(20L, "lead updatedBy"));
    lead.setOwnerId(new IdName(20L, "lead owner"));
    lead.setId(55L);
    Metadata metadata = new Metadata(10L, 5L, LEAD, null, null, EntityAction.CREATED);

    var leadEvent = new LeadEvent(lead, null, metadata);
    EmailAction emailAction = new EmailAction(1L, from, participants, participants, participants);
    User aUser = UserStub.aUser(20L, 5L, true, true, true, true, true)
        .withName("Updated lead");
    Workflow workflow = new Workflow();
    workflow.setCreatedBy(aUser);
    emailAction.setWorkflow(workflow);
    given(userService.getUserDetails(20L, authService.getAuthenticationToken())).willReturn(Mono.just(aUser));
    //when
    emailActionService.processEmailAction(emailAction, leadEvent);

    //then
    verifyNoMoreInteractions(emailActionEventPublisher);
  }

  private Object getFrom(String type, long id, String name, String email) throws IOException {
    String from = "{\"type\":\"" + type + "\",\"entity\":\"user\",\"entityId\":" + id + ",\"name\":\"" + name + "\",\"email\":\"" + email + "\"}";
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.readValue(from, Object.class);
  }

}
