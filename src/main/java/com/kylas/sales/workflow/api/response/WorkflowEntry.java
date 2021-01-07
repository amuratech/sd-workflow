package com.kylas.sales.workflow.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kylas.sales.workflow.common.dto.User;
import com.kylas.sales.workflow.domain.user.Action;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WorkflowEntry {

  private final long id;
  private final String name;
  private final EntityType entityType;
  private final User createdBy;
  private final User updatedBy;
  private final Date createdAt;
  private final Date updatedAt;
  private final Date lastTriggeredAt;
  private final Long triggerCount;
  @JsonProperty("recordActions")
  private final Action allowedActions;
  private final boolean active;
}
