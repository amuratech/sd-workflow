package com.kylas.sales.workflow.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Tenant {

  private String accountName;
  private String industry;
  private String address;
  private String city;
  private String state;
  private String country;
  private String zip;
  private String language;
  private String currency;
  private String timezone;
  private String companyName;
  private String website;
}
