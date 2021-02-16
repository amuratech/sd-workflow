package com.kylas.sales.workflow.domain.processor.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import lombok.Getter;
import lombok.NonNull;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskRelation {

  @NonNull
  private final EntityType targetEntityType;
  private final long targetEntityId;

  @JsonCreator
  public TaskRelation(@JsonProperty("targetEntityType") EntityType targetEntityType, @JsonProperty("targetEntityId") long targetEntityId) {
    this.targetEntityType = targetEntityType;
    this.targetEntityId = targetEntityId;
  }
}
