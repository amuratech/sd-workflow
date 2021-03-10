package com.kylas.sales.workflow.domain.workflow.action.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylas.sales.workflow.domain.processor.EmailActionDetail;
import com.kylas.sales.workflow.domain.processor.email.Participant;
import com.kylas.sales.workflow.domain.processor.lead.Email;
import com.kylas.sales.workflow.domain.service.UserService;
import com.kylas.sales.workflow.mq.EmailActionEventPublisher;
import com.kylas.sales.workflow.mq.event.EmailActionEvent;
import com.kylas.sales.workflow.mq.event.EntityEvent;
import com.kylas.sales.workflow.mq.event.Metadata;
import com.kylas.sales.workflow.security.AuthService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailActionService {

  private final List<EmailActionType> emailActionTypes = List
      .of(EmailActionType.USER, EmailActionType.CONTACT, EmailActionType.LEAD, EmailActionType.CUSTOM_EMAIL, EmailActionType.WORKFLOW_CREATOR,
          EmailActionType.WORKFLOW_UPDATER);

  private final EmailActionEventPublisher emailActionEventPublisher;
  private final UserService userService;
  private final AuthService authService;

  @Autowired
  public EmailActionService(EmailActionEventPublisher emailActionEventPublisher, UserService userService,
      AuthService authService) {
    this.emailActionEventPublisher = emailActionEventPublisher;
    this.userService = userService;
    this.authService = authService;
  }

  public void processEmailAction(EmailAction emailAction, EntityEvent event, Metadata metadata) {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      com.kylas.sales.workflow.domain.workflow.action.email.Participant from = objectMapper
          .readValue(objectMapper.writeValueAsString(emailAction.getFrom()),
              com.kylas.sales.workflow.domain.workflow.action.email.Participant.class);

      EmailActionEvent emailActionEvent = new EmailActionEvent(getParticipant(from, event.getEmailActionDetail(), event.getEntityId()).getId(),
          getResolvedListOfParticipants(emailAction.getTo(),
              event.getEmailActionDetail(), event.getEntityId()), getResolvedListOfParticipants(emailAction.getCc(),
          event.getEmailActionDetail(), event.getEntityId()), getResolvedListOfParticipants(emailAction.getBcc(),
          event.getEmailActionDetail(), event.getEntityId()));
      emailActionEventPublisher.publishEmailActionEvent(null);
    } catch (JsonProcessingException e) {
      log.error(e.getMessage(), e);
    }
  }

  private List<Participant> convertAvailableEmailsToParticipants(com.kylas.sales.workflow.domain.workflow.action.email.Participant participant,
      EmailActionDetail emailActionDetail, long entityId) {
    return null == emailActionDetail.getEmails() || emailActionDetail.getEmails().length == 0 ? Collections.emptyList()
        : Arrays.stream(emailActionDetail.getEmails())
            .map(email -> new Participant(participant.getEntity(), entityId, emailActionDetail.getName(), email.getValue()))
            .collect(Collectors.toList());
  }

  private Participant getParticipant(com.kylas.sales.workflow.domain.workflow.action.email.Participant participant,
      EmailActionDetail emailActionDetail, long entityId) {

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
      case RECORD_PRIMARY_EMAIL:
        return convertRecordPrimaryEmailToParticipant(participant, emailActionDetail, entityId);
    }
    return null;
  }

  private Participant getParticipantWithUpdatedInfo(long userId, String entity) {
    return userService.getUserDetails(userId, authService.getAuthenticationToken())
        .map(user -> new Participant(entity, userId, user.getName(), user.getEmail())).block();
  }

  private Participant convertRecordPrimaryEmailToParticipant(com.kylas.sales.workflow.domain.workflow.action.email.Participant participant,
      EmailActionDetail emailActionDetail, long entityId) {
    return null == emailActionDetail.getEmails() || emailActionDetail.getEmails().length == 0 ? null :
        Arrays.stream(emailActionDetail.getEmails()).filter(Email::isPrimary).findFirst()
            .map(email -> new Participant(participant.getEntity(), entityId, emailActionDetail.getName(), email.getValue())).orElse(null);
  }

  private List<Participant> getResolvedListOfParticipants(List<com.kylas.sales.workflow.domain.workflow.action.email.Participant> participants,
      EmailActionDetail emailActionDetail, long entityId) {

    List<Participant> participantsList = new ArrayList<>();
    participants.stream().forEach(participant -> {
      if (participant.getType().equals(EmailActionType.ALL_AVAILABLE_EMAILS)) {
        participantsList.addAll(convertAvailableEmailsToParticipants(participant, emailActionDetail, entityId))
      } else {
        participantsList.add(getParticipant(participant, emailActionDetail, entityId));
      }
    });
    return participantsList;
  }
}
