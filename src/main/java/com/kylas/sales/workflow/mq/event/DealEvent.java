package com.kylas.sales.workflow.mq.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kylas.sales.workflow.domain.processor.Actionable;
import com.kylas.sales.workflow.domain.processor.EmailActionDetail;
import com.kylas.sales.workflow.domain.processor.deal.DealDetail;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DealEvent implements EntityEvent {

  private DealDetail entity;
  private DealDetail oldEntity;
  private Metadata metadata;

  @JsonCreator
  public DealEvent(
      @JsonProperty("entity") DealDetail entity,
      @JsonProperty("oldEntity") DealDetail oldEntity,
      @JsonProperty("metadata") Metadata metadata
  ) {
    this.entity = entity;
    this.oldEntity = oldEntity;
    this.metadata = metadata;
  }

  public static String getDealCreatedEventName() {
    return "deal.created.v2";
  }

  public static String getDealUpdatedEventName() {
    return "deal.updated.v2";
  }

  @Override
  public DealDetail getEntity() {
    return this.entity;
  }

  @Override
  public DealDetail getOldEntity() {
    return this.oldEntity;
  }

  @Override
  public Metadata getMetadata() {
    return this.metadata;
  }

  @Override
  public Actionable getActualEntity() {
    return new DealDetail();
  }

  @Override
  public EmailActionDetail getEmailActionDetail() {
    return new EmailActionDetail(this.entity.getName(), this.entity.getCreatedBy(), this.entity.getUpdatedBy(), this.entity.getOwnedBy(), null,
        this.entity.getAssociatedContacts());
  }

  @Override
  public long getEntityId() {
    return this.entity.getId();
  }
}
