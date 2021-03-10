package com.kylas.sales.workflow.domain.processor.contact;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kylas.sales.workflow.domain.processor.lead.Email;
import com.kylas.sales.workflow.domain.processor.lead.PhoneNumber;
import java.util.Date;
import java.util.Map;
import lombok.Getter;

@Getter
public class ContactResponse {

  private Long id;
  private Long ownerId;
  private Long salutation;
  private String firstName;
  private String lastName;

  private PhoneNumber[] phoneNumbers;
  private final Email[] emails;

  private Map<String, Object> customFieldValues;

  private boolean dnd;
  private String timezone;
  private String address;
  private String city;
  private String state;
  private String zipcode;
  private String country;

  private String facebook;
  private String twitter;
  private String linkedin;


  //Professional
  private Long company;
  private String department;
  private String designation;
  private boolean stakeholder;

  //Internal
  private Date convertedAt;
  private Long convertedBy;

  public ContactResponse(Email[] emails) {
    this.emails = emails;
  }

  public ContactResponse(@JsonProperty("id") Long id, @JsonProperty("ownerId") Long ownerId, @JsonProperty("salutation") Long salutation,
      @JsonProperty("firstName") String firstName, @JsonProperty("lastName") String lastName,
      @JsonProperty("phoneNumbers") PhoneNumber[] phoneNumbers, @JsonProperty("emails") Email[] emails,
      @JsonProperty("customFieldValues") Map<String, Object> customFieldValues,
      @JsonProperty("dnd") boolean dnd, @JsonProperty("timezone") String timezone, @JsonProperty("address") String address,
      @JsonProperty("city") String city, @JsonProperty("state") String state, @JsonProperty("zipcode") String zipcode,
      @JsonProperty("country") String country,
      @JsonProperty("facebook") String facebook, @JsonProperty("twitter") String twitter, @JsonProperty("linkedin") String linkedin,
      @JsonProperty("company") Long company,
      @JsonProperty("department") String department, @JsonProperty("designation") String designation,
      @JsonProperty("stakeholder") boolean stakeholder, @JsonProperty("convertedAt") Date convertedAt,
      @JsonProperty("convertedBy") Long convertedBy) {

    this.id = id;
    this.ownerId = ownerId;
    this.salutation = salutation;
    this.firstName = firstName;
    this.lastName = lastName;
    this.phoneNumbers = phoneNumbers;
    this.emails = emails;
    this.customFieldValues = customFieldValues;
    this.dnd = dnd;
    this.timezone = timezone;
    this.address = address;
    this.city = city;
    this.state = state;
    this.zipcode = zipcode;
    this.country = country;
    this.facebook = facebook;
    this.twitter = twitter;
    this.linkedin = linkedin;
    this.company = company;
    this.department = department;
    this.designation = designation;
    this.stakeholder = stakeholder;
    this.convertedAt = convertedAt;
    this.convertedBy = convertedBy;
  }

  public static ContactResponse withEmails(Email[] emails) {
    return new ContactResponse(emails);
  }
}
