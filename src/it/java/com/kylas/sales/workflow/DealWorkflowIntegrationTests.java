package com.kylas.sales.workflow;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static java.util.stream.IntStream.range;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylas.sales.workflow.api.response.WorkflowSummary;
import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@AutoConfigureWireMock(port = 9090)
@ContextConfiguration(initializers = {TestDatabaseInitializer.class})
public class DealWorkflowIntegrationTests {

  @Autowired
  Environment environment;
  private final String authenticationToken =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZWxsIiwiZGF0YSI6eyJleHBpcmVzSW4iOjQzMTk5LCJleHBpcnkiOjE1NzY0OTM3MTAsInRva2VuVHlwZSI6ImJlYXJlciIsInBlcm1pc3Npb25zIjpbeyJpZCI6NCwibmFtZSI6ImxlYWQiLCJkZXNjcmlwdGlvbiI6ImhhcyBhY2Nlc3MgdG8gbGVhZCByZXNvdXJjZSIsImxpbWl0cyI6LTEsInVuaXRzIjoiY291bnQiLCJhY3Rpb24iOnsicmVhZCI6dHJ1ZSwid3JpdGUiOnRydWUsInVwZGF0ZSI6dHJ1ZSwiZGVsZXRlIjp0cnVlLCJlbWFpbCI6ZmFsc2UsImNhbGwiOmZhbHNlLCJzbXMiOmZhbHNlLCJ0YXNrIjp0cnVlLCJub3RlIjp0cnVlLCJyZWFkQWxsIjp0cnVlLCJ1cGRhdGVBbGwiOnRydWV9fSx7ImlkIjo3LCJuYW1lIjoid29ya2Zsb3ciLCJkZXNjcmlwdGlvbiI6ImhhcyBhY2Nlc3MgdG8gd29ya2Zsb3cgcmVzb3VyY2UiLCJsaW1pdHMiOi0xLCJ1bml0cyI6ImNvdW50IiwiYWN0aW9uIjp7InJlYWQiOnRydWUsIndyaXRlIjp0cnVlLCJ1cGRhdGUiOnRydWUsImRlbGV0ZSI6dHJ1ZSwiZW1haWwiOmZhbHNlLCJjYWxsIjpmYWxzZSwic21zIjpmYWxzZSwidGFzayI6ZmFsc2UsIm5vdGUiOmZhbHNlLCJyZWFkQWxsIjp0cnVlLCJ1cGRhdGVBbGwiOnRydWV9fV0sInVzZXJJZCI6IjEyIiwidXNlcm5hbWUiOiJ0b255QHN0YXJrLmNvbSIsInRlbmFudElkIjoiNTUifX0.xzQ-Ih5N1nllqkqgsBdS1NJgqhgNVJi1hiSZcuOrxp8";

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  public void givenWorkflowRequest_withEntityDeal_shouldCreate() throws IOException {
    // given
    stubFor(
        get("/iam/v1/users/12")
            .withHeader(AUTHORIZATION, matching("Bearer " + authenticationToken))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody(
                        getResourceAsString("/contracts/user/responses/user-details-by-id.json"))));

    var workflowRequest =
        getResourceAsString("/contracts/workflow/api/create-workflow-request-with-entity-deal.json");
    // when
    var workflowResponse =
        buildWebClient()
            .post()
            .uri("/v1/workflows")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(workflowRequest)
            .retrieve()
            .bodyToMono(String.class);
    // then
    StepVerifier.create(workflowResponse)
        .assertNext(
            json -> {
              try {
                WorkflowSummary workflowSummary = objectMapper.readValue(json, WorkflowSummary.class);
                Assertions.assertThat(workflowSummary.getId()).isGreaterThan(0);
              } catch (JsonProcessingException e) {
                fail(e.getMessage());
              }
            })
        .verifyComplete();
  }

  @Test
  @Sql("/test-scripts/integration/insert-deal-workflow-for-integration-test.sql")
  public void givenWorkflowUpdateRequest_withEntityDeal_shouldUpdate() throws IOException {

    stubFor(
        get("/iam/v1/users/12")
            .withHeader(AUTHORIZATION, matching("Bearer " + authenticationToken))
            .willReturn(
                aResponse()
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                    .withStatus(200)
                    .withBody(
                        getResourceAsString("/contracts/user/responses/user-details-by-id.json"))));

    stubFor(
        get("/iam/v1/users/20003")
            .withHeader(AUTHORIZATION, matching("Bearer " + authenticationToken))
            .willReturn(
                aResponse()
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                    .withStatus(200)
                    .withBody(
                        getResourceAsString("/contracts/user/responses/user-details-by-id.json"))));

    stubFor(
        get("/search/v1/summaries/pipeline?id=11")
            .withHeader(AUTHORIZATION, matching("Bearer " + authenticationToken))
            .willReturn(
                aResponse()
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                    .withStatus(200)
                    .withBody(
                        getResourceAsString("/contracts/pipeline/responses/pipeline-details.json"))));

    var workflowRequest =
        getResourceAsString("/contracts/workflow/api/update-workflow-request-with-entity-deal.json");
    // when
    var workflowResponse =
        buildWebClient()
            .put()
            .uri("/v1/workflows/301")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(workflowRequest)
            .retrieve()
            .bodyToMono(String.class);
    // then
    var expectedResponse =
        getResourceAsString("/contracts/workflow/api/integration/update-deal-workflow-response.json");
    StepVerifier.create(workflowResponse)
        .assertNext(
            json -> {
              try {
                JSONAssert.assertEquals(
                    expectedResponse,
                    json,
                    new CustomComparator(
                        JSONCompareMode.STRICT,
                        getCustomizationsToBeIgnored()));
              } catch (JSONException e) {
                fail(e.getMessage());
              }
            })
        .verifyComplete();
  }

  @Test
  public void givenDealWorkflowRequest_withCreateTaskAction_shouldCreate() throws IOException {
    // given
    stubFor(
        get("/iam/v1/users/12")
            .withHeader(AUTHORIZATION, matching("Bearer " + authenticationToken))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody(
                        getResourceAsString("/contracts/user/responses/user-details-by-id.json"))));
    var workflowRequest =
        getResourceAsString("/contracts/workflow/api/task/deal-create-workflow-with-create-task-action-request.json");
    // when
    var workflowResponse =
        buildWebClient()
            .post()
            .uri("/v1/workflows")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(workflowRequest)
            .retrieve()
            .bodyToMono(String.class);
    // then
    var expectedResponse =
        getResourceAsString("/contracts/workflow/api/create-workflow-response.json");
    StepVerifier.create(workflowResponse)
        .assertNext(
            json -> {
              try {
                WorkflowSummary workflowSummary = objectMapper.readValue(json, WorkflowSummary.class);
                Assertions.assertThat(workflowSummary.getId()).isGreaterThan(0);
              } catch (IOException e) {
                fail(e.getMessage());
              }
            })
        .verifyComplete();
  }

  @Test
  @Sql("/test-scripts/integration/insert-deal-workflow-for-integration-test.sql")
  public void givenDealWorkflowUpdateRequest_withNewTaskAction_shouldUpdateWorkflow() throws IOException {

    stubFor(
        get("/iam/v1/users/12")
            .withHeader(AUTHORIZATION, matching("Bearer " + authenticationToken))
            .willReturn(
                aResponse()
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                    .withStatus(200)
                    .withBody(
                        getResourceAsString("/contracts/user/responses/user-details-by-id.json"))));

    stubFor(
        get("/iam/v1/users/20003")
            .withHeader(AUTHORIZATION, matching("Bearer " + authenticationToken))
            .willReturn(
                aResponse()
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                    .withStatus(200)
                    .withBody(
                        getResourceAsString("/contracts/user/responses/user-details-by-id.json"))));

    var workflowRequest =
        getResourceAsString("/contracts/workflow/api/task/deal-update-workflow-with-create-task-action-request.json");
    // when
    var workflowResponse =
        buildWebClient()
            .put()
            .uri("/v1/workflows/301")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(workflowRequest)
            .retrieve()
            .bodyToMono(String.class);
    // then
    var expectedResponse =
        getResourceAsString("/contracts/workflow/api/task/deal-update-workflow-with-create-task-action-response.json");
    StepVerifier.create(workflowResponse)
        .assertNext(
            json -> {
              try {
                JSONAssert.assertEquals(
                    expectedResponse,
                    json,
                    new CustomComparator(
                        JSONCompareMode.STRICT,
                        getCustomizationsToBeIgnored()));
              } catch (JSONException e) {
                fail(e.getMessage());
              }
            })
        .verifyComplete();
  }

  private Customization[] getCustomizationsToBeIgnored() {
    List<Customization> list = new ArrayList<>();
    range(0, 9).forEach(element -> {
      list.add(new Customization("actions[" + element + "].id", (o1, o2) -> true));
      list.add(new Customization("actions[" + element + "].type", (o1, o2) -> true));
      list.add(new Customization("actions[" + element + "].payload", (o1, o2) -> true));
    });

    list.add(new Customization("lastTriggeredAt", (o1, o2) -> true));
    list.add(new Customization("updatedAt", (o1, o2) -> true));
    list.add(new Customization("createdAt", (o1, o2) -> true));
    return list.toArray(new Customization[0]);
  }

  @NotNull
  private WebClient buildWebClient() {
    var port = environment.getProperty("local.server.port");

    return WebClient.builder()
        .baseUrl("http://localhost:" + port)
        .defaultHeader(HttpHeaders.ACCEPT, APPLICATION_JSON_VALUE)
        .defaultHeader(AUTHORIZATION, "Bearer " + authenticationToken)
        .build();
  }

  private String getResourceAsString(String resourcePath) throws IOException {
    var resource = new ClassPathResource(resourcePath);
    var file = resource.getFile();
    return FileUtils.readFileToString(file, "UTF-8");
  }
}
