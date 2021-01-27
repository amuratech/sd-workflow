package com.kylas.sales.workflow.domain.processor.deal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class Money {

  private final Long currencyId;
  private final Double value;

  @JsonCreator
  public Money(@JsonProperty("currencyId") Long currencyId, @JsonProperty("value") Double value) {
    this.currencyId = currencyId;
    this.value = value;
  }
}
