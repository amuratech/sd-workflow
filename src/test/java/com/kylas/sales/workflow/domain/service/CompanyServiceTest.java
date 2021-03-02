package com.kylas.sales.workflow.domain.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@AutoConfigureWireMock(port = 9082)
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = {TestDatabaseInitializer.class})
@TestPropertySource(properties = {"client.company.basePath=http://localhost:9082"})
public class CompanyServiceTest {

  @Autowired
  private CompanyService companyService;

  @Test
  public void givenCompanyId_shouldReturnCompany() throws JsonProcessingException {
    var companyId = 1L;

    var tokenString =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZWxsIiwiZGF0YSI6eyJleHBpcmVzSW4iOjQzMTk5LCJleHBpcnkiOjE1NzY0OTM3MTAsInRva2VuVHlwZSI6ImJlYXJlciIsInBlcm1pc3Npb25zIjpbeyJpZCI6NCwibmFtZSI6ImxlYWQiLCJkZXNjcmlwdGlvbiI6ImhhcyBhY2Nlc3MgdG8gbGVhZCByZXNvdXJjZSIsImxpbWl0cyI6LTEsInVuaXRzIjoiY291bnQiLCJhY3Rpb24iOnsicmVhZCI6dHJ1ZSwid3JpdGUiOnRydWUsInVwZGF0ZSI6dHJ1ZSwiZGVsZXRlIjp0cnVlLCJlbWFpbCI6ZmFsc2UsImNhbGwiOmZhbHNlLCJzbXMiOmZhbHNlLCJ0YXNrIjp0cnVlLCJub3RlIjp0cnVlLCJyZWFkQWxsIjp0cnVlLCJ1cGRhdGVBbGwiOnRydWV9fSx7ImlkIjo3LCJuYW1lIjoicHJvZHVjdHMtc2VydmljZXMiLCJkZXNjcmlwdGlvbiI6ImhhcyBhY2Nlc3MgdG8gdGVhbSByZXNvdXJjZSIsImxpbWl0cyI6LTEsInVuaXRzIjoiY291bnQiLCJhY3Rpb24iOnsicmVhZCI6dHJ1ZSwid3JpdGUiOnRydWUsInVwZGF0ZSI6dHJ1ZSwiZGVsZXRlIjp0cnVlLCJlbWFpbCI6ZmFsc2UsImNhbGwiOmZhbHNlLCJzbXMiOmZhbHNlLCJ0YXNrIjpmYWxzZSwibm90ZSI6ZmFsc2UsInJlYWRBbGwiOnRydWUsInVwZGF0ZUFsbCI6dHJ1ZX19XSwidXNlcklkIjoiMTIiLCJ1c2VybmFtZSI6InRvbnlAc3RhcmsuY29tIiwidGVuYW50SWQiOiIxNCJ9fQ.Ac464gjHy_U0_B9r6NNr02zlrMXWSWQO1Fmp9jhm8ok";

    String responseBody =
        new ObjectMapper()
            .writeValueAsString(
                new HashMap<String, String>() {
                  {
                    put("id", "1");
                    put("name", "Company 1");
                  }
                });

    stubFor(
        get("/v1/companies/" + companyId)
            .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer " + tokenString))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody(responseBody)));

    var companyMono = companyService.getCompanyById(companyId, tokenString);
    StepVerifier.create(companyMono)
        .assertNext(
            company -> {
              assertThat(company.getId()).isEqualTo(1L);
              assertThat(company.getName()).isEqualTo("Company 1");
            })
        .verifyComplete();
  }
}
