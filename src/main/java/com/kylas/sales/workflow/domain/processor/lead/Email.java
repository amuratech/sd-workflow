package com.kylas.sales.workflow.domain.processor.lead;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class Email implements Serializable {

  private EmailType type;
  private String value;
  private boolean isPrimary;

  @JsonCreator
  public Email(@JsonProperty("type") EmailType type, @JsonProperty("value") String value, @JsonProperty("isPrimary") boolean isPrimary) {
    this.type = type;
    this.value = value;
    this.isPrimary = isPrimary;
  }
}
