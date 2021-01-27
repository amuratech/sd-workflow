package com.kylas.sales.workflow.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.kylas.sales.workflow.security.jwt.Authentication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class InternalAuthProviderTest {

  private final InternalAuthProvider internalAuthProvider = new InternalAuthProvider("test-jwt-key");

  @Test
  public void shouldCreateInternalAuthToken() {
    String authToken = internalAuthProvider.create(1L, 10L);
    assertThat(authToken).isNotEmpty();
  }

  @Test
  public void shouldLoginWithUser() {
    internalAuthProvider.loginWith(1L, 10L);
    Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isNotNull();
    assertThat(authentication.getUserId()).isEqualTo("1");
    assertThat(authentication.getTenantId()).isEqualTo("10");
  }

}