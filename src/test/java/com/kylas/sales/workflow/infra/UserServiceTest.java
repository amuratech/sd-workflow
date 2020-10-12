package com.kylas.sales.workflow.infra;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import com.kylas.sales.workflow.domain.service.UserService;
import com.kylas.sales.workflow.security.jwt.Authentication;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;


@ExtendWith(MockitoExtension.class)
@AutoConfigureWireMock(port = 9081)
@SpringBootTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(initializers = {TestDatabaseInitializer.class})
@TestPropertySource(properties = {"client.iam.basePath=http://localhost:9081"})
class UserServiceTest {

  @Autowired
  private UserService userService;

  @Test
  public void givenUserId_shouldGetItsDetails() throws IOException {
    // given
    var userId = 12L;

    var tokenString =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZWxsIiwiZGF0YSI6eyJleHBpcmVzSW4iOjQzMTk5LCJleHBpcnkiOjE1NzY0OTM3MTAsInRva2VuVHlwZSI6ImJlYXJlciIsInBlcm1pc3Npb25zIjpbeyJpZCI6NCwibmFtZSI6ImxlYWQiLCJkZXNjcmlwdGlvbiI6ImhhcyBhY2Nlc3MgdG8gbGVhZCByZXNvdXJjZSIsImxpbWl0cyI6LTEsInVuaXRzIjoiY291bnQiLCJhY3Rpb24iOnsicmVhZCI6dHJ1ZSwid3JpdGUiOnRydWUsInVwZGF0ZSI6dHJ1ZSwiZGVsZXRlIjp0cnVlLCJlbWFpbCI6ZmFsc2UsImNhbGwiOmZhbHNlLCJzbXMiOmZhbHNlLCJ0YXNrIjp0cnVlLCJub3RlIjp0cnVlLCJyZWFkQWxsIjp0cnVlLCJ1cGRhdGVBbGwiOnRydWV9fSx7ImlkIjo3LCJuYW1lIjoicHJvZHVjdHMtc2VydmljZXMiLCJkZXNjcmlwdGlvbiI6ImhhcyBhY2Nlc3MgdG8gdGVhbSByZXNvdXJjZSIsImxpbWl0cyI6LTEsInVuaXRzIjoiY291bnQiLCJhY3Rpb24iOnsicmVhZCI6dHJ1ZSwid3JpdGUiOnRydWUsInVwZGF0ZSI6dHJ1ZSwiZGVsZXRlIjp0cnVlLCJlbWFpbCI6ZmFsc2UsImNhbGwiOmZhbHNlLCJzbXMiOmZhbHNlLCJ0YXNrIjpmYWxzZSwibm90ZSI6ZmFsc2UsInJlYWRBbGwiOnRydWUsInVwZGF0ZUFsbCI6dHJ1ZX19XSwidXNlcklkIjoiMTIiLCJ1c2VybmFtZSI6InRvbnlAc3RhcmsuY29tIiwidGVuYW50SWQiOiIxNCJ9fQ.Ac464gjHy_U0_B9r6NNr02zlrMXWSWQO1Fmp9jhm8ok";

    var authentication = Authentication.from(tokenString, "test");
    SecurityContextHolder.getContext().setAuthentication(authentication);

    stubFor(
        get("/v1/users/" + userId)
            .withHeader(
                HttpHeaders.AUTHORIZATION,
                matching(
                    "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZWxsIiwiZGF0YSI6eyJleHBpcmVzSW4iOjQzMTk5LCJleHBpcnkiOjE1NzY0OTM3MTAsInRva2VuVHlwZSI6ImJlYXJlciIsInBlcm1pc3Npb25zIjpbeyJpZCI6NCwibmFtZSI6ImxlYWQiLCJkZXNjcmlwdGlvbiI6ImhhcyBhY2Nlc3MgdG8gbGVhZCByZXNvdXJjZSIsImxpbWl0cyI6LTEsInVuaXRzIjoiY291bnQiLCJhY3Rpb24iOnsicmVhZCI6dHJ1ZSwid3JpdGUiOnRydWUsInVwZGF0ZSI6dHJ1ZSwiZGVsZXRlIjp0cnVlLCJlbWFpbCI6ZmFsc2UsImNhbGwiOmZhbHNlLCJzbXMiOmZhbHNlLCJ0YXNrIjp0cnVlLCJub3RlIjp0cnVlLCJyZWFkQWxsIjp0cnVlLCJ1cGRhdGVBbGwiOnRydWV9fSx7ImlkIjo3LCJuYW1lIjoicHJvZHVjdHMtc2VydmljZXMiLCJkZXNjcmlwdGlvbiI6ImhhcyBhY2Nlc3MgdG8gdGVhbSByZXNvdXJjZSIsImxpbWl0cyI6LTEsInVuaXRzIjoiY291bnQiLCJhY3Rpb24iOnsicmVhZCI6dHJ1ZSwid3JpdGUiOnRydWUsInVwZGF0ZSI6dHJ1ZSwiZGVsZXRlIjp0cnVlLCJlbWFpbCI6ZmFsc2UsImNhbGwiOmZhbHNlLCJzbXMiOmZhbHNlLCJ0YXNrIjpmYWxzZSwibm90ZSI6ZmFsc2UsInJlYWRBbGwiOnRydWUsInVwZGF0ZUFsbCI6dHJ1ZX19XSwidXNlcklkIjoiMTIiLCJ1c2VybmFtZSI6InRvbnlAc3RhcmsuY29tIiwidGVuYW50SWQiOiIxNCJ9fQ.Ac464gjHy_U0_B9r6NNr02zlrMXWSWQO1Fmp9jhm8ok"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody(
                        getResponsePayload("/contracts/user/responses/user-details-by-id.json"))));

    // when
    var userDetailsMono = userService.getUserDetails(userId, tokenString);

    // then
    StepVerifier.create(userDetailsMono)
        .assertNext(
            user -> {
              assertThat(user.getId() == 12L);
              assertThat(user.getName().equalsIgnoreCase("Tony Stark"));
              var permissions = new ArrayList<>(user.getPermissions());
              assertThat(permissions).hasSize(2);

              var workflowPermission =
                  permissions.stream()
                      .filter(permission -> permission.getName().equalsIgnoreCase("workflow"))
                      .findFirst()
                      .get();
              assertThat(workflowPermission.getId()).isEqualTo(12L);
              assertThat(workflowPermission.getName()).isEqualTo("workflow");
              assertThat(workflowPermission.getDescription())
                  .isEqualTo("has permission to workflow resource");

              var workflowAction = workflowPermission.getAction();
              assertThat(workflowAction.canRead()).isTrue();
            })
        .verifyComplete();
  }
  private String getResponsePayload(String resourcePath) throws IOException {
    return IOUtils.toString(this.getClass().getResourceAsStream(resourcePath), "UTF-8");
  }
}
