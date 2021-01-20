package com.kylas.sales.workflow.domain.processor.contact;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.kylas.sales.workflow.domain.processor.lead.Email;
import com.kylas.sales.workflow.domain.processor.lead.IdName;
import com.kylas.sales.workflow.domain.processor.lead.PhoneNumber;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@Setter
@JsonInclude(Include.NON_NULL)
public class ContactDetail {

  private Long id;
  private Long tenantId;
  private IdName ownerId;

  private String firstName;
  private String lastName;
  private String name;
  private IdName salutation;
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
  private String linkedIn;

  private String company;
  private String designation;
  private String department;
  private Boolean stakeholder;
  private Date convertedAt;
  private IdName convertedBy;

  private Boolean deleted;
  private Integer version;
  private Date createdAt;
  private Date updatedAt;
  private IdName createdBy;
  private IdName updatedBy;
}
