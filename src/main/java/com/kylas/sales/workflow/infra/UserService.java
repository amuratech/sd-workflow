package com.kylas.sales.workflow.infra;

import com.kylas.sales.workflow.domain.user.User;
import com.kylas.sales.workflow.security.jwt.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  public User getLoggedInUser() {
    var authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
    var userId = Long.parseLong(authentication.getUserId());
    var tenantId = Long.parseLong(authentication.getTenantId());
    return new User(userId, tenantId, authentication.getPermissions());
  }

  public String getAuthenticationToken() {
    var authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
    return authentication.getJwtToken();
  }
}
