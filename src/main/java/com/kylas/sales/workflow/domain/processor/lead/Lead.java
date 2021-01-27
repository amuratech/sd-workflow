package com.kylas.sales.workflow.domain.processor.lead;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kylas.sales.workflow.domain.processor.Actionable;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@Setter
@JsonInclude(Include.NON_NULL)
public class Lead implements Serializable, Actionable {

  private Long id;
  private Long tenantId;
  private Long ownerId;


  private String firstName;
  private String lastName;
  private String name;
  private Long salutation;


  private String timezone;

  private String address;
  private String city;
  private String state;
  private String zipcode;
  private String country;

  private String department;
  private Boolean dnd;

  private PhoneNumber[] phoneNumbers;
  private String[] photoUrls;
  private Email[] emails;


  private String facebook;
  private String twitter;
  private String linkedIn;

  private Long pipeline;
  private Long pipelineStage;
  private String pipelineStageReason;

  // company
  private String companyName;
  private String companyAddress;
  private String companyCity;
  private String companyState;
  private String companyZipcode;
  private String companyCountry;
  private Integer companyEmployees;
  private Double companyAnnualRevenue;
  private String companyWebsite;
  private String companyIndustry;
  private String companyBusinessType;
  private PhoneNumber[] companyPhones;

  private String requirementName;
  private String requirementCurrency;
  private Double requirementBudget;
  private Date expectedClosureOn;

  private List<Product> products;

  private ConversionAssociation conversionAssociation;
  private Date convertedAt;
  private Long convertedBy;

  private String designation;
  private Long campaign;
  private Long source;

  private ForecastingType forecastingType;

  private Map<String, Object> customFieldValues;
  private Long importedBy;

  private Boolean deleted;
  private Integer version;
  private Date createdAt;
  private Date updatedAt;
  private Long createdBy;
  private Long updatedBy;

  private Date actualClosureDate;

  @Override
  @JsonIgnore
  public String getEventName() {
    return "workflow.lead.update";
  }
}
