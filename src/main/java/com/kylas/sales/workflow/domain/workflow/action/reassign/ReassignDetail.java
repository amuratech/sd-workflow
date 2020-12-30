package com.kylas.sales.workflow.domain.workflow.action.reassign;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.kylas.sales.workflow.domain.processor.Actionable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@AllArgsConstructor
public class ReassignDetail implements Serializable, Actionable {

  private final Long entityId;
  private final Long ownerId;

  @Override
  @JsonIgnore
  public String getEventName() {
    return "workflow.lead.reassign";
  }
}
