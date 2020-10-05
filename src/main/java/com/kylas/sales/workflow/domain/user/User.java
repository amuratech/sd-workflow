package com.kylas.sales.workflow.domain.user;

import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class User {

  @Getter(AccessLevel.NONE)
    private static final String WORKFLOW_PERMISSION_NAME = "workflow";

  private final long id;
  private final long tenantId;

  @Getter(AccessLevel.NONE)
  private final Set<Permission> permissions;

  public User(long id, long tenantId, Set<Permission> permissions) {
    this.id = id;
    this.tenantId = tenantId;
    this.permissions = permissions;
  }
}
