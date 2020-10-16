package com.kylas.sales.workflow.domain.processor.lead;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PhoneNumber implements Serializable {

  private PhoneType type;
  private String code;
  private String value;
  private String dialCode;
  private boolean isPrimary;

  @JsonCreator
  public PhoneNumber(
      @JsonProperty("type") PhoneType type,
      @JsonProperty("code") String code,
      @JsonProperty("value") String value,
      @JsonProperty("dialCode") String dialCode,
      @JsonProperty("isPrimary") boolean isPrimary) {
    this.type = type;
    this.code = code;
    this.value = value;
    this.dialCode = dialCode;
    this.isPrimary = isPrimary;
  }
}
