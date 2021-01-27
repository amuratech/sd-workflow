package com.kylas.sales.workflow.domain.processor.lead;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConversionAssociation {

  private Long id;
  private Long dealId;
  private Long contactId;
  private Long companyId;
  private Long tenantId;

  @JsonCreator
  public ConversionAssociation(
      @JsonProperty("id") Long id,
      @JsonProperty("dealId") Long dealId,
      @JsonProperty("contactId") Long contactId,
      @JsonProperty("tenantId") Long tenantId,
      @JsonProperty("companyId") Long companyId) {
    this.id = id;
    this.dealId = dealId;
    this.contactId = contactId;
    this.tenantId = tenantId;
    this.companyId = companyId;
  }
}
