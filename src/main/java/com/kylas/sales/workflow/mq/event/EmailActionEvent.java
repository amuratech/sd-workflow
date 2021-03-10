package com.kylas.sales.workflow.mq.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kylas.sales.workflow.domain.processor.email.Participant;
import java.util.List;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailActionEvent {

  private final long senderId;
  private final List<Participant> to;
  private final List<Participant> cc;
  private final List<Participant> bcc;
  private final Participant relatedTo;
  private final long emailTemplateId;
  private final long userId;
  private final long tenantId;

  @JsonCreator
  public EmailActionEvent(@JsonProperty("senderId") long senderId, @JsonProperty("to") List<Participant> to, @JsonProperty("cc") List<Participant> cc,
      @JsonProperty("bcc") List<Participant> bcc, @JsonProperty("relatedTo") Participant relatedTo,
      @JsonProperty("emailTemplateId") long emailTemplateId, @JsonProperty("userId") long userId,
      @JsonProperty("tenantId") long tenantId) {
    this.senderId = senderId;
    this.to = to;
    this.cc = cc;
    this.bcc = bcc;
    this.relatedTo = relatedTo;
    this.emailTemplateId = emailTemplateId;
    this.userId = userId;
    this.tenantId = tenantId;
  }

  @JsonIgnore
  public static String getEventName() {
    return "workflow.email.send";
  }
}
