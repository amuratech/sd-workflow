package com.kylas.sales.workflow.security.jwt;

import io.jsonwebtoken.MalformedJwtException;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class JwtTokenRequestFilter extends OncePerRequestFilter {

  private final String jwtSigningKey;

  @Autowired
  public JwtTokenRequestFilter(@Value("${security.jwt.key}") String jwtSigningKey) {
    this.jwtSigningKey = jwtSigningKey;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String jwtToken = null;
    if (request.getParameter("token") != null) {
      log.debug("token is present in query parameter");
      jwtToken = request.getParameter("token");
    }

    var authorizationHeader = request.getHeader("Authorization");
    if (authorizationHeader != null && authorizationHeader.length() > 0) {
      try {
        jwtToken = authorizationHeader.split("Bearer ")[1];
        log.debug("token is present in header");
      } catch (Exception e) {
        log.warn("got error while parsing token in header", e);
      }
    }

    if (jwtToken != null) {
      try {

        var authentication = Authentication.from(jwtToken, jwtSigningKey);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        addToLogContext(authentication);
      } catch (MalformedJwtException exception) {
        log.warn("jwt token is malformed so ignoring it", exception);
      }
    }
    chain.doFilter(request, response);
  }

  private void addToLogContext(Authentication accessToken) {
    MDC.put("user.tenant.id", accessToken.getTenantId());
    MDC.put("user.id", accessToken.getUserId());
  }
}
