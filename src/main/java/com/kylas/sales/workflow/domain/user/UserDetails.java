package com.kylas.sales.workflow.domain.user;

import com.kylas.sales.workflow.domain.processor.lead.IdName;
import com.kylas.sales.workflow.domain.processor.lead.PhoneNumber;
import java.util.Map;
import lombok.Getter;

@Getter
public class UserDetails {

  private final Long id;
  private final String firstName;
  private final String lastName;
  private final String designation;
  private final String department;
  private final String currency;
  private final String timezone;
  private final String signature;
  private final String language;
  private final boolean active;
  private final String email;
  private final PhoneNumber[] phoneNumbers;
  private final IdName salutation;
  private final IdName createdBy;
  private final IdName updatedBy;

  private UserDetails(Long id, String firstName, String lastName, String designation, String department, String currency, String timezone,
      String signature, String language, boolean active, String email, PhoneNumber[] phoneNumbers,
      IdName salutation, IdName createdBy,
      IdName updatedBy) {
    this.id = id;
    this.firstName = firstName;
    this.lastName = lastName;
    this.designation = designation;
    this.department = department;
    this.currency = currency;
    this.timezone = timezone;
    this.signature = signature;
    this.language = language;
    this.active = active;
    this.email = email;
    this.phoneNumbers = phoneNumbers;
    this.salutation = salutation;
    this.createdBy = createdBy;
    this.updatedBy = updatedBy;
  }

  public static UserDetails from(User user) {
    var idNameStore = user.getMetaData().getIdNameStore();
    var salutation = getIdName(idNameStore, user.getSalutation(), "salutation");
    var createdBy = getIdName(idNameStore, user.getCreatedBy(), "createdBy");
    var updatedBy = getIdName(idNameStore, user.getUpdatedBy(), "updatedBy");

    return new UserDetails(
        user.getId(),
        user.getFirstName(),
        user.getLastName(),
        user.getDesignation(),
        user.getDepartment(),
        user.getCurrency(),
        user.getTimezone(),
        user.getSignature(),
        user.getLanguage(),
        user.isActive(),
        user.getEmail(),
        user.getPhoneNumbers(),
        salutation,
        createdBy,
        updatedBy);
  }

  private static IdName getIdName(Map<String, Map<Long, String>> idNameStore, Long id, String fieldName) {
    if (id != null && idNameStore.containsKey(fieldName)) {
      return new IdName(id, idNameStore.get(fieldName).get(id));
    }
    return new IdName(id, null);
  }
}
