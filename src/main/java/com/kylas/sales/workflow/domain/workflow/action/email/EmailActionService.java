package com.kylas.sales.workflow.domain.workflow.action.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylas.sales.workflow.domain.processor.EmailActionDetail;
import com.kylas.sales.workflow.domain.processor.contact.ContactResponse;
import com.kylas.sales.workflow.domain.processor.email.Participant;
import com.kylas.sales.workflow.domain.processor.lead.Email;
import com.kylas.sales.workflow.domain.service.ContactService;
import com.kylas.sales.workflow.domain.service.UserService;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.mq.EmailActionEventPublisher;
import com.kylas.sales.workflow.mq.event.EmailActionEvent;
import com.kylas.sales.workflow.mq.event.EntityEvent;
import com.kylas.sales.workflow.mq.event.Metadata;
import com.kylas.sales.workflow.security.AuthService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailActionService {

  private final List<EmailActionType> emailActionTypes = List
      .of(EmailActionType.CONTACT, EmailActionType.LEAD, EmailActionType.CUSTOM_EMAIL,
          EmailActionType.TENANT_EMAIL);

  private final EmailActionEventPublisher emailActionEventPublisher;
  private final UserService userService;
  private final AuthService authService;
  private final ContactService contactService;

  @Autowired
  public EmailActionService(EmailActionEventPublisher emailActionEventPublisher, UserService userService,
      AuthService authService, ContactService contactService) {
    this.emailActionEventPublisher = emailActionEventPublisher;
    this.userService = userService;
    this.authService = authService;
    this.contactService = contactService;
  }

  public void processEmailAction(EmailAction emailAction, EntityEvent event) {
    ObjectMapper objectMapper = new ObjectMapper();
    try {

      com.kylas.sales.workflow.domain.workflow.action.email.Participant from = objectMapper
          .readValue(objectMapper.writeValueAsString(emailAction.getFrom()),
              com.kylas.sales.workflow.domain.workflow.action.email.Participant.class);

      Metadata metadata = event.getMetadata();
      Workflow workflow = emailAction.getWorkflow();

      long entityId = event.getEntityId();
      Participant emailFrom = getParticipant(from, workflow, event);
      List<Participant> to = getResolvedListOfParticipants(emailAction.getTo(), workflow, event);
      List<Participant> cc = getResolvedListOfParticipants(emailAction.getCc(), workflow, event);
      List<Participant> bcc = getResolvedListOfParticipants(emailAction.getBcc(), workflow, event);
      Participant relatedTo = getRelatedTo(event);

      EmailActionEvent emailActionEvent = new EmailActionEvent(emailFrom.getId(), to, cc, bcc, relatedTo, emailAction.getEmailTemplateId(),
          metadata.getUserId(), metadata.getTenantId());

      if (!to.isEmpty()) {
        log.info("publishing email action event for entityId {} ", entityId);
        emailActionEventPublisher.publishEmailActionEvent(emailActionEvent);
      }

    } catch (JsonProcessingException e) {
      log.error(e.getMessage(), e);
    }
  }

  private List<Participant> getResolvedListOfParticipants(List<com.kylas.sales.workflow.domain.workflow.action.email.Participant> participants,
      Workflow workflow, EntityEvent event) {

    EmailActionDetail emailActionDetail = event.getEmailActionDetail();

    List<Participant> participantsList = new ArrayList<>();

    if (participants == null || participants.isEmpty()) {
      return participantsList;
    }

    participants.stream()
        .filter(participant -> participant.getType().equals(EmailActionType.ALL_ASSOCIATED_CONTACTS))
        .forEach(participant -> participantsList
            .addAll(convertDealRecordPrimaryEmailToParticipants(participant, emailActionDetail, event.getEntityId())));

    participants.stream()
        .filter(participant -> participant.getType().equals(EmailActionType.ALL_AVAILABLE_EMAILS))
        .forEach(participant -> participantsList.addAll(convertAvailableEmailsToParticipants(participant, emailActionDetail, event.getEntityId())));

    participants.stream()
        .filter(participant -> !participant.getType().equals(EmailActionType.ALL_AVAILABLE_EMAILS) && !participant.getType()
            .equals(EmailActionType.ALL_ASSOCIATED_CONTACTS))
        .forEach(participant -> {
          Participant localParticipant = getParticipant(participant, workflow, event);
          if (localParticipant != null) {
            participantsList.add(localParticipant);
          }
        });

    return participantsList;
  }

  private Participant getParticipant(com.kylas.sales.workflow.domain.workflow.action.email.Participant participant, Workflow workflow,
      EntityEvent event) {
    EmailActionDetail emailActionDetail = event.getEmailActionDetail();

    if (emailActionTypes.contains(participant.getType())) {
      return new Participant(participant.getEntity(), participant.getEntityId(), participant.getName(), participant.getEmail());
    }

    switch (participant.getType()) {
      case RECORD_OWNER:
        return getParticipantWithUpdatedInfo(emailActionDetail.getOwnedBy().getId(), participant.getEntity());
      case RECORD_CREATED_BY:
        return getParticipantWithUpdatedInfo(emailActionDetail.getCreatedBy().getId(), participant.getEntity());
      case RECORD_UPDATED_BY:
        return getParticipantWithUpdatedInfo(emailActionDetail.getUpdatedBy().getId(), participant.getEntity());
      case WORKFLOW_CREATOR:
        return getParticipantWithUpdatedInfo(workflow.getCreatedBy().getId(), participant.getEntity());
      case WORKFLOW_UPDATER:
        return getParticipantWithUpdatedInfo(workflow.getUpdatedBy().getId(), participant.getEntity());
      case USER:
        return getParticipantWithUpdatedInfo(participant.getEntityId(), participant.getEntity());
      case RECORD_PRIMARY_EMAIL:
        return convertRecordPrimaryEmailToParticipant(participant, event.getEmailActionDetail(), event.getEntityId());
      default:
        throw new IllegalStateException("Unexpected value: " + participant.getType());
    }
  }

  private Participant getParticipantWithUpdatedInfo(long userId, String entity) {
    return userService.getUserDetails(userId, authService.getAuthenticationToken())
        .map(user -> new Participant(entity, userId, user.getName(), user.getEmail()))
        .block();
  }

  private Participant convertRecordPrimaryEmailToParticipant(com.kylas.sales.workflow.domain.workflow.action.email.Participant participant,
      EmailActionDetail emailActionDetail, long entityId) {

    return null == emailActionDetail.getEmails() || emailActionDetail.getEmails().length == 0 ? null :
        Arrays.stream(emailActionDetail.getEmails())
            .filter(Email::isPrimary)
            .findFirst()
            .map(email -> new Participant(participant.getEntity(), entityId, emailActionDetail.getName(), email.getValue()))
            .orElse(new Participant(participant.getEntity(), participant.getEntityId(), participant.getName(), participant.getEmail()));
  }

  private List<Participant> convertAvailableEmailsToParticipants(com.kylas.sales.workflow.domain.workflow.action.email.Participant participant,
      EmailActionDetail emailActionDetail, long entityId) {
    return null == emailActionDetail.getEmails() || emailActionDetail.getEmails().length == 0 ? Collections.emptyList()
        : Arrays.stream(emailActionDetail.getEmails())
            .map(email -> new Participant(participant.getEntity(), entityId, emailActionDetail.getName(), email.getValue()))
            .collect(Collectors.toList());
  }

  private List<Participant> convertDealRecordPrimaryEmailToParticipants(com.kylas.sales.workflow.domain.workflow.action.email.Participant
      participant,
      EmailActionDetail emailActionDetail, long entityId) {

    List<Participant> participantList = new ArrayList<>();

    emailActionDetail.getAssociatedContacts().stream()
        .map(contact -> contactService.getContactById(contact.getId(), authService.getAuthenticationToken()))
        .map(ContactResponse::getEmails)
        .map(emails -> Arrays.stream(emails)
            .filter(Email::isPrimary)
            .findFirst()
            .orElse(null)
        ).collect(Collectors.toList())
        .stream().filter(Objects::nonNull)
        .forEach(email -> participantList.add(new Participant(participant.getEntity(), entityId, emailActionDetail.getName(), email.getValue())));

    return participantList;
  }

  private Participant getRelatedTo(EntityEvent event) {

    EntityType entityType = event.getMetadata().getEntityType();
    EmailActionDetail emailActionDetail = event.getEmailActionDetail();

    if (entityType.equals(EntityType.DEAL)) {
      return new Participant(entityType.name().toLowerCase(), event.getEntityId(), emailActionDetail.getName(), null);
    }

    String primaryEmail =
        null == emailActionDetail.getEmails() || emailActionDetail.getEmails().length == 0 ? null : Arrays.stream(emailActionDetail.getEmails())
            .filter(Email::isPrimary)
            .findFirst()
            .map(Email::getValue)
            .orElse(null);

    return new Participant(entityType.name().toLowerCase(), event.getEntityId(), emailActionDetail.getName(), primaryEmail);
  }
}
