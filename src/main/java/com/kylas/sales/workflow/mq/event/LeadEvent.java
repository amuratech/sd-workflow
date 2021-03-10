package com.kylas.sales.workflow.mq.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kylas.sales.workflow.domain.processor.EmailActionDetail;
import com.kylas.sales.workflow.domain.processor.lead.Lead;
import com.kylas.sales.workflow.domain.processor.lead.LeadDetail;
import java.util.Collections;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeadEvent implements EntityEvent {

  private LeadDetail entity;
  private LeadDetail oldEntity;
  private Metadata metadata;

  @JsonCreator
  public LeadEvent(
      @JsonProperty("entity") LeadDetail entity,
      @JsonProperty("oldEntity") LeadDetail oldEntity,
      @JsonProperty("metadata") Metadata metadata
  ) {
    this.entity = entity;
    this.oldEntity = oldEntity;
    this.metadata = metadata;
  }

  public static String getLeadCreatedEventName() {
    return "sales.lead.created.v2";
  }

  public static String getLeadUpdatedEventName() {
    return "sales.lead.updated.v2";
  }

  @Override
  public LeadDetail getEntity() {
    return this.entity;
  }

  @Override
  public LeadDetail getOldEntity() {
    return this.oldEntity;
  }

  @Override
  public Metadata getMetadata() {
    return this.metadata;
  }

  public Lead getActualEntity() {
    return new Lead();
  }

  @Override
  public EmailActionDetail getEmailActionDetail() {
    return new EmailActionDetail(this.entity.getName(), this.entity.getCreatedBy(), this.entity.getUpdatedBy(), this.entity.getOwnerId(),
        this.entity.getEmails(),
        Collections
            .emptyList());
  }

  @Override
  public long getEntityId() {
    return this.entity.getId();
  }
}
