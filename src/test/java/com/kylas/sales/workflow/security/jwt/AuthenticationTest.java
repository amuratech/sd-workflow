package com.kylas.sales.workflow.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuthenticationTest {

  @Test
  public void givenJwtStringToken_shouldBeAbleToConstructAuthentication() {
    // given
    var tokenString =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZWxsIiwiZGF0YSI6eyJleHBpcmVzSW4iOjQzMTk5LCJleHBpcnkiOjE1NzY0OTM3MTAsInRva2VuVHlwZSI6ImJlYXJlciIsInBlcm1pc3Npb25zIjpbeyJpZCI6NCwibmFtZSI6ImxlYWQiLCJkZXNjcmlwdGlvbiI6ImhhcyBhY2Nlc3MgdG8gbGVhZCByZXNvdXJjZSIsImxpbWl0cyI6LTEsInVuaXRzIjoiY291bnQiLCJhY3Rpb24iOnsicmVhZCI6dHJ1ZSwid3JpdGUiOnRydWUsInVwZGF0ZSI6dHJ1ZSwiZGVsZXRlIjp0cnVlLCJlbWFpbCI6ZmFsc2UsImNhbGwiOmZhbHNlLCJzbXMiOmZhbHNlLCJ0YXNrIjp0cnVlLCJub3RlIjp0cnVlLCJyZWFkQWxsIjp0cnVlLCJ1cGRhdGVBbGwiOnRydWV9fSx7ImlkIjo3LCJuYW1lIjoidGVhbSIsImRlc2NyaXB0aW9uIjoiaGFzIGFjY2VzcyB0byB0ZWFtIHJlc291cmNlIiwibGltaXRzIjotMSwidW5pdHMiOiJjb3VudCIsImFjdGlvbiI6eyJyZWFkIjp0cnVlLCJ3cml0ZSI6dHJ1ZSwidXBkYXRlIjp0cnVlLCJkZWxldGUiOnRydWUsImVtYWlsIjpmYWxzZSwiY2FsbCI6ZmFsc2UsInNtcyI6ZmFsc2UsInRhc2siOmZhbHNlLCJub3RlIjpmYWxzZSwicmVhZEFsbCI6dHJ1ZSwidXBkYXRlQWxsIjp0cnVlfX1dLCJ1c2VySWQiOiIxMiIsInVzZXJuYW1lIjoidG9ueUBzdGFyay5jb20iLCJ0ZW5hbnRJZCI6IjE0In19.DffNQd2aIKCgpo3jVzY6nIZLH8DWWYNJ_U3W9DSeom8";

    // when
    var authentication = Authentication.from(tokenString, "test");

    // then
    assertThat(authentication.getPermissions()).hasSize(2);
    assertThat(authentication.getUserId()).isEqualTo("12");
    assertThat(authentication.getJwtToken()).isEqualTo(tokenString);
    assertThat(authentication.getTenantId()).isEqualTo("14");
  }
}
