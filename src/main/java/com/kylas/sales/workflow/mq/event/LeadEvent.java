package com.kylas.sales.workflow.mq.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kylas.sales.workflow.domain.processor.lead.Lead;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeadEvent {
  private Lead entity;
  private Lead oldEntity;
  private Metadata metadata;

  @JsonCreator
  public LeadEvent(
      @JsonProperty("entity") Lead entity,
      @JsonProperty("oldEntity") Lead oldEntity,
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

}
