package com.kylas.sales.workflow.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kylas.sales.workflow.common.dto.WorkflowAction;
import com.kylas.sales.workflow.common.dto.WorkflowCondition;
import com.kylas.sales.workflow.common.dto.WorkflowTrigger;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import java.util.Set;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowRequest {

  @NotNull
  private String name;
  private String description;
  @NotNull
  private EntityType entityType;
  @NotNull
  private WorkflowTrigger trigger;
  @NotNull
  private WorkflowCondition condition;
  @NotEmpty
  private Set<WorkflowAction> actions;
  private boolean active = true;
}
