package com.kylas.sales.workflow.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kylas.sales.workflow.domain.workflow.TriggerType;
import com.kylas.sales.workflow.domain.workflow.TriggerFrequency;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
public class TriggerRequest {
  private TriggerType name;
  private TriggerFrequency triggerFrequency;

}
