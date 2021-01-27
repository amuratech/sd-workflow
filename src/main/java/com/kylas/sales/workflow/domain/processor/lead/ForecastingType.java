package com.kylas.sales.workflow.domain.processor.lead;

import lombok.Getter;

@Getter
public enum ForecastingType {
  OPEN,
  CLOSED_WON,
  CLOSED_UNQUALIFIED,
  CLOSED_LOST,
  CLOSED;

}
