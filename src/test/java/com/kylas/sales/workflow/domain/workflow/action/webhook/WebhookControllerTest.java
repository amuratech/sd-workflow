package com.kylas.sales.workflow.domain.workflow.action.webhook;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(initializers = TestDatabaseInitializer.class)
class WebhookControllerTest {

  @Autowired
  Environment environment;
  @MockBean
  WebhookService webhookService;

  private WebClient buildWebClient() {
    var port = environment.getProperty("local.server.port");

    return WebClient.builder()
        .baseUrl("http://localhost:" + port)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(
            HttpHeaders.AUTHORIZATION,
            "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZWxsIiwiZGF0YSI6eyJleHBpcmVzSW4iOjQzMTk5LCJleHBpcnkiOjE1NzY0OTM3MTAsInRva2VuVHlwZSI6ImJlYXJlciIsInBlcm1pc3Npb25zIjpbXSwidXNlcklkIjoiMTIiLCJ1c2VybmFtZSI6InRvbnlAc3RhcmsuY29tIiwidGVuYW50SWQiOiI1NSJ9fQ.m5WpWXnmhZs6838cyxeD-U1FYcm3COnYf8DcIaHLFMM")
        .build();
  }

  @Test
  public void getWebhookConfigurations_shouldFetchIt() {
    //when
    Flux<String> response = buildWebClient()
        .get()
        .uri("/v1/workflows/webhook/config")
        .retrieve()
        .bodyToFlux(String.class);
    //then
    StepVerifier.create(response)
        .verifyComplete();
    verify(webhookService, times(1)).getConfigurations();
  }

}