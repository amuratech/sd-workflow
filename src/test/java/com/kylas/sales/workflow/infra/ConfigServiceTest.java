package com.kylas.sales.workflow.infra;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okForContentType;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import com.kylas.sales.workflow.domain.entity.EntityDefinition;
import com.kylas.sales.workflow.domain.service.ConfigService;
import com.kylas.sales.workflow.security.jwt.Authentication;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;


@ExtendWith(MockitoExtension.class)
@AutoConfigureWireMock(port = 9081)
@SpringBootTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(initializers = {TestDatabaseInitializer.class})
@TestPropertySource(properties = {"client.config.basePath=http://localhost:9081"})
class ConfigServiceTest {

  @Autowired
  private ConfigService configService;

  @Test
  public void givenEntityFieldsRequest_shouldFetchIt() throws IOException {
    // given
    var tokenString =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZWxsIiwiZGF0YSI6eyJleHBpcmVzSW4iOjQzMTk5LCJleHBpcnkiOjE1NzY0OTM3MTAsInRva2VuVHlwZSI6ImJlYXJlciIsInBlcm1pc3Npb25zIjpbeyJpZCI6NCwibmFtZSI6ImxlYWQiLCJkZXNjcmlwdGlvbiI6ImhhcyBhY2Nlc3MgdG8gbGVhZCByZXNvdXJjZSIsImxpbWl0cyI6LTEsInVuaXRzIjoiY291bnQiLCJhY3Rpb24iOnsicmVhZCI6dHJ1ZSwid3JpdGUiOnRydWUsInVwZGF0ZSI6dHJ1ZSwiZGVsZXRlIjp0cnVlLCJlbWFpbCI6ZmFsc2UsImNhbGwiOmZhbHNlLCJzbXMiOmZhbHNlLCJ0YXNrIjp0cnVlLCJub3RlIjp0cnVlLCJyZWFkQWxsIjp0cnVlLCJ1cGRhdGVBbGwiOnRydWV9fSx7ImlkIjo3LCJuYW1lIjoidXNlciIsImRlc2NyaXB0aW9uIjoiaGFzIGFjY2VzcyB0byB0ZWFtIHJlc291cmNlIiwibGltaXRzIjotMSwidW5pdHMiOiJjb3VudCIsImFjdGlvbiI6eyJyZWFkIjp0cnVlLCJ3cml0ZSI6dHJ1ZSwidXBkYXRlIjp0cnVlLCJkZWxldGUiOnRydWUsImVtYWlsIjpmYWxzZSwiY2FsbCI6ZmFsc2UsInNtcyI6ZmFsc2UsInRhc2siOmZhbHNlLCJub3RlIjpmYWxzZSwicmVhZEFsbCI6dHJ1ZSwidXBkYXRlQWxsIjp0cnVlfX1dLCJ1c2VySWQiOiIxMiIsInVzZXJuYW1lIjoidG9ueUBzdGFyay5jb20iLCJ0ZW5hbnRJZCI6IjE0In19.bZ51HLZcWkN33Tth6sDQuMkutIjEvP-wkz2PTGIuBlE";

    var authentication = Authentication.from(tokenString, "test");
    SecurityContextHolder.getContext().setAuthentication(authentication);

    stubFor(
        get("/v1/entities/user/fields")
            .withHeader(
                HttpHeaders.AUTHORIZATION,
                matching(
                    "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZWxsIiwiZGF0YSI6eyJleHBpcmVzSW4iOjQzMTk5LCJleHBpcnkiOjE1NzY0OTM3MTAsInRva2VuVHlwZSI6ImJlYXJlciIsInBlcm1pc3Npb25zIjpbeyJpZCI6NCwibmFtZSI6ImxlYWQiLCJkZXNjcmlwdGlvbiI6ImhhcyBhY2Nlc3MgdG8gbGVhZCByZXNvdXJjZSIsImxpbWl0cyI6LTEsInVuaXRzIjoiY291bnQiLCJhY3Rpb24iOnsicmVhZCI6dHJ1ZSwid3JpdGUiOnRydWUsInVwZGF0ZSI6dHJ1ZSwiZGVsZXRlIjp0cnVlLCJlbWFpbCI6ZmFsc2UsImNhbGwiOmZhbHNlLCJzbXMiOmZhbHNlLCJ0YXNrIjp0cnVlLCJub3RlIjp0cnVlLCJyZWFkQWxsIjp0cnVlLCJ1cGRhdGVBbGwiOnRydWV9fSx7ImlkIjo3LCJuYW1lIjoidXNlciIsImRlc2NyaXB0aW9uIjoiaGFzIGFjY2VzcyB0byB0ZWFtIHJlc291cmNlIiwibGltaXRzIjotMSwidW5pdHMiOiJjb3VudCIsImFjdGlvbiI6eyJyZWFkIjp0cnVlLCJ3cml0ZSI6dHJ1ZSwidXBkYXRlIjp0cnVlLCJkZWxldGUiOnRydWUsImVtYWlsIjpmYWxzZSwiY2FsbCI6ZmFsc2UsInNtcyI6ZmFsc2UsInRhc2siOmZhbHNlLCJub3RlIjpmYWxzZSwicmVhZEFsbCI6dHJ1ZSwidXBkYXRlQWxsIjp0cnVlfX1dLCJ1c2VySWQiOiIxMiIsInVzZXJuYW1lIjoidG9ueUBzdGFyay5jb20iLCJ0ZW5hbnRJZCI6IjE0In19.bZ51HLZcWkN33Tth6sDQuMkutIjEvP-wkz2PTGIuBlE"))
            .willReturn(
                okForContentType(
                    MediaType.APPLICATION_JSON_VALUE,
                    getResponsePayload("/contracts/config/response/get-user-fields.json"))));

    // when
    Flux<EntityDefinition> fields = configService.getFields("user", tokenString);

    // then
    StepVerifier.create(fields)
        .assertNext(firstNameField -> {
          assertThat(firstNameField.getName()).isEqualTo("firstName");
          assertThat(firstNameField.getDisplayName()).isEqualTo("First Name");
        })
        .assertNext(lastNameField -> {
          assertThat(lastNameField.getName()).isEqualTo("lastName");
          assertThat(lastNameField.getDisplayName()).isEqualTo("Last Name");
        })
        .verifyComplete();
  }

  private String getResponsePayload(String resourcePath) throws IOException {
    return IOUtils.toString(this.getClass().getResourceAsStream(resourcePath), StandardCharsets.UTF_8);
  }
}
