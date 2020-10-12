package com.kylas.sales.workflow.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
public class EditPropertyRequest {

  private String name;
  private String value;

}
