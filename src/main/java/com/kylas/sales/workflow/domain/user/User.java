package com.kylas.sales.workflow.domain.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micrometer.core.instrument.util.StringUtils;
import java.util.HashSet;
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
      @JsonProperty("name") String name) {
    this.id = id;
    this.tenantId = tenantId;
    this.permissions = permissions;
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
}
