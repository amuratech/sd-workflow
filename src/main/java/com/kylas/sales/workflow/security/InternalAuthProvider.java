package com.kylas.sales.workflow.security;

import com.kylas.sales.workflow.domain.user.Action;
import com.kylas.sales.workflow.domain.user.Permission;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class InternalAuthProvider {

  private final String jwtSigningKey;

  @Autowired
  public InternalAuthProvider(@Value("${security.jwt.key}") String jwtSigningKey) {
    this.jwtSigningKey = jwtSigningKey;
  }

  public void loginWith(Long userId, Long tenantId) {
    String authToken = create(userId, tenantId);
    var authentication = com.kylas.sales.workflow.security.jwt.Authentication.from(authToken, jwtSigningKey);
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  public String create(Long userId, Long tenantId) {
    CoreAccessToken coreAccessToken = new CoreAccessToken(userId, tenantId);

    return Jwts.builder()
        .setIssuer("sell")
        .claim("data", coreAccessToken)
        .signWith(
            SignatureAlgorithm.HS256,
            jwtSigningKey.getBytes(StandardCharsets.UTF_8))
        .compact();
  }

  @Getter
  static class CoreAccessToken {

    final long expiresIn;
    final Date expiry;
    final String tokenType;
    final Set<Permission> permissions;
    final String userId;
    final String tenantId;

    CoreAccessToken(Long userId, Long tenantId) {
      Date now = new Date();
      this.expiry = DateUtils.addSeconds(now, 10);
      this.expiresIn = 10000;
      this.tokenType = "Bearer";
      this.userId = userId.toString();
      this.tenantId = tenantId.toString();
      this.permissions = new HashSet<>() {{
        var readAction = new Action();
        readAction.setRead(true);
        readAction.setReadAll(true);
        add(new Permission(3, "user", "has access to user resource", readAction));
      }};
    }
  }
}
