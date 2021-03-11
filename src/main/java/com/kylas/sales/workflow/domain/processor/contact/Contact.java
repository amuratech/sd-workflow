package com.kylas.sales.workflow.domain.processor.contact;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.kylas.sales.workflow.domain.processor.Actionable;
import com.kylas.sales.workflow.domain.processor.lead.Email;
import com.kylas.sales.workflow.domain.processor.lead.PhoneNumber;
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
public class Contact implements Serializable, Actionable {

  private Long id;
  private Long tenantId;
  private Long ownerId;

  private String firstName;
  private String lastName;
  private String name;
  private Long salutation;
  private String address;
  private String city;
  private String state;
  private String zipcode;
  private String country;

  private Boolean dnd;
  private String timezone;
  private PhoneNumber[] phoneNumbers;
  private Email[] emails;

  private String facebook;
  private String twitter;
  private String linkedin;

  private Long company;
  private String designation;
  private String department;
  private Boolean stakeholder;
  private Date convertedAt;
  private Long convertedBy;

  private Boolean deleted;
  private Integer version;
  private Date createdAt;
  private Date updatedAt;
  private Long createdBy;
  private Long updatedBy;

  private Map<String, Object> customFieldValues;
  private List<Long> associatedDeals;


  @Override
  @JsonIgnore
  public String getEventName() {
    return "workflow.contact.update";
  }
}