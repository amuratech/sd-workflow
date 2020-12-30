package com.kylas.sales.workflow.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.common.dto.User;
import com.kylas.sales.workflow.common.dto.WorkflowCondition;
import com.kylas.sales.workflow.common.dto.WorkflowTrigger;
import com.kylas.sales.workflow.domain.user.Action;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WorkflowDetail {

  private final long id;
  private final String name;
  private final String description;
  private final EntityType entityType;
  private final WorkflowTrigger trigger;
  private final WorkflowCondition condition;
  private final List<ActionResponse> actions;
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
