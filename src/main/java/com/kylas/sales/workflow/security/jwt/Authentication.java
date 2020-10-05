package com.kylas.sales.workflow.security.jwt;

import static java.util.Collections.emptyList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylas.sales.workflow.domain.user.Permission;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@Getter
public class Authentication extends UsernamePasswordAuthenticationToken {
  private final Set<Permission> permissions;
  private final String tenantId;
  private String userId;
  private final String jwtToken;

  private Authentication(
      String userId, String tenantId, Set<Permission> permissions, String jwtToken) {
    super(userId, jwtToken, emptyList());
    this.tenantId = tenantId;
    this.permissions = permissions;
    this.userId = userId;
    this.jwtToken = jwtToken;
  }

  public static Authentication from(String jwtTokenString, String jwtSigningKey) {
    Jws<Claims> claims =
        Jwts.parser()
            .setSigningKey(jwtSigningKey.getBytes(StandardCharsets.UTF_8))
            .parseClaimsJws(jwtTokenString);
    var body = claims.getBody().get("data");
    var mapper = new ObjectMapper();
    var jwt = mapper.convertValue(body, Jwt.class);

    return new Authentication(jwt.userId, jwt.tenantId, jwt.permissions, jwtTokenString);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class Jwt {
    private final Date expiry;
    private final Set<Permission> permissions = new HashSet<>();
    private final String userId;
    private final String tenantId;

    @JsonCreator
    Jwt(
        @JsonProperty("expiry") Date expiry,
        @JsonProperty("permissions") Set<Permission> permissions,
        @JsonProperty("userId") String userId,
        @JsonProperty("tenantId") String tenantId) {
      this.userId = userId;
      this.tenantId = tenantId;
      this.expiry = expiry;
      this.permissions.addAll(permissions);
    }
  }
}
