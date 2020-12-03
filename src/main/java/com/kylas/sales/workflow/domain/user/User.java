package com.kylas.sales.workflow.domain.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kylas.sales.workflow.domain.processor.lead.PhoneNumber;
import io.micrometer.core.instrument.util.StringUtils;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor
public class User {

  private static final String WORKFLOW_PERMISSION_NAME = "workflow";
  @Id
  private long id;
  private String name;
  private long tenantId;
  @Transient
  private Set<Permission> permissions = new HashSet<>();
  @Transient
  private String firstName;
  @Transient
  private String lastName;
  @Transient
  private String designation;
  @Transient
  private String department;
  @Transient
  private String timezone;
  @Transient
  private String language;
  @Transient
  private String currency;
  @Transient
  private String signature;
  @Transient
  private boolean active;
  @Transient
  private String email;
  @Transient
  private PhoneNumber[] phoneNumbers;
  @Transient
  private Long salutation;
  @Transient
  private Long createdBy;
  @Transient
  private Long updatedBy;
  @Transient
  private Metadata metaData;

  private User(long id, String name, long tenantId, Set<Permission> permissions) {
    this.id = id;
    this.name = name;
    this.tenantId = tenantId;
    this.permissions = permissions;
  }

  public User(long id, long tenantId, Set<Permission> permissions) {
    this.id = id;
    this.tenantId = tenantId;
    this.permissions = permissions;
    this.name = null;
  }

  @JsonCreator
  public User(
      @JsonProperty("id") long id,
      @JsonProperty("tenantId") long tenantId,
      @JsonProperty("permissions") Set<Permission> permissions,
      @JsonProperty("firstName") String firstName,
      @JsonProperty("lastName") String lastName,
      @JsonProperty("name") String name,
      @JsonProperty("department") String department,
      @JsonProperty("designation") String designation,
      @JsonProperty("currency") String currency,
      @JsonProperty("timezone") String timezone,
      @JsonProperty("language") String language,
      @JsonProperty("signature") String signature,
      @JsonProperty("active") boolean active,
      @JsonProperty("email") String email,
      @JsonProperty("phoneNumbers") PhoneNumber[] phoneNumbers,
      @JsonProperty("salutation") long salutation,
      @JsonProperty("createdBy") long createdBy,
      @JsonProperty("updatedBy") long updatedBy,
      @JsonProperty("metaData") Metadata metaData) {
    this.id = id;
    this.tenantId = tenantId;
    this.permissions = permissions;
    this.firstName = firstName;
    this.lastName = lastName;
    this.department = department;
    this.designation = designation;
    this.timezone = timezone;
    this.currency = currency;
    this.language = language;
    this.signature = signature;
    this.metaData = metaData;
    this.active = active;
    this.email = email;
    this.phoneNumbers = phoneNumbers;
    this.salutation = salutation;
    this.createdBy = createdBy;
    this.updatedBy = updatedBy;
    this.name =
        StringUtils.isBlank(name)
            ? StringUtils.isBlank(firstName) ? lastName : firstName + " " + lastName
            : name;
  }

  public User withName(String name) {
    return new User(id, name, tenantId, permissions);
  }

  public User withName(String firstName, String lastName) {
    var name = StringUtils.isBlank(firstName) ? lastName : firstName + " " + lastName;
    return new User(id, name, tenantId, permissions);
  }

  public User withPermissions(Set<Permission> permissions) {
    return new User(id, name, tenantId, permissions);
  }

  public User withTenantId(long tenantId) {
    return new User(id, name, tenantId, permissions);
  }

  public boolean canCreateWorkflow() {
    return permissions.stream()
        .anyMatch(
            permission ->
                permission.getName().equalsIgnoreCase(WORKFLOW_PERMISSION_NAME)
                    && permission.getAction().canWrite());
  }

  public boolean canQueryHisWorkflow() {
    return permissions.stream()
        .anyMatch(
            permission ->
                permission.getName().equalsIgnoreCase(WORKFLOW_PERMISSION_NAME)
                    && permission.getAction().canRead());
  }

  public boolean canQueryAllWorkflow() {
    return permissions.stream()
        .anyMatch(
            permission ->
                permission.getName().equalsIgnoreCase(WORKFLOW_PERMISSION_NAME)
                    && permission.getAction().canReadAll());
  }

  public boolean canUpdateHisWorkflow() {
    return permissions.stream()
        .anyMatch(
            permission ->
                permission.getName().equalsIgnoreCase(WORKFLOW_PERMISSION_NAME)
                    && permission.getAction().canUpdate());
  }

  public boolean canUpdateAllWorkflow() {
    return permissions.stream()
        .anyMatch(
            permission ->
                permission.getName().equalsIgnoreCase(WORKFLOW_PERMISSION_NAME)
                    && permission.getAction().canUpdateAll());
  }

  @Getter
  public static class Metadata {

    private final Map<String, Map<Long, String>> idNameStore;

    @JsonCreator
    public Metadata(@JsonProperty("idNameStore") Map<String, Map<Long, String>> idNameStore) {
      this.idNameStore = idNameStore;
    }
  }
}
