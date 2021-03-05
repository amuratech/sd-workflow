package com.kylas.sales.workflow;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylas.sales.workflow.api.response.WorkflowSummary;
import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import java.io.IOException;
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
public class WorkflowIntegrationTests {

  @Autowired
  Environment environment;
  private final String authenticationToken =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZWxsIiwiZGF0YSI6eyJleHBpcmVzSW4iOjQzMTk5LCJleHBpcnkiOjE1NzY0OTM3MTAsInRva2VuVHlwZSI6ImJlYXJlciIsInBlcm1pc3Npb25zIjpbeyJpZCI6NCwibmFtZSI6ImxlYWQiLCJkZXNjcmlwdGlvbiI6ImhhcyBhY2Nlc3MgdG8gbGVhZCByZXNvdXJjZSIsImxpbWl0cyI6LTEsInVuaXRzIjoiY291bnQiLCJhY3Rpb24iOnsicmVhZCI6dHJ1ZSwid3JpdGUiOnRydWUsInVwZGF0ZSI6dHJ1ZSwiZGVsZXRlIjp0cnVlLCJlbWFpbCI6ZmFsc2UsImNhbGwiOmZhbHNlLCJzbXMiOmZhbHNlLCJ0YXNrIjp0cnVlLCJub3RlIjp0cnVlLCJyZWFkQWxsIjp0cnVlLCJ1cGRhdGVBbGwiOnRydWV9fSx7ImlkIjo3LCJuYW1lIjoid29ya2Zsb3ciLCJkZXNjcmlwdGlvbiI6ImhhcyBhY2Nlc3MgdG8gd29ya2Zsb3cgcmVzb3VyY2UiLCJsaW1pdHMiOi0xLCJ1bml0cyI6ImNvdW50IiwiYWN0aW9uIjp7InJlYWQiOnRydWUsIndyaXRlIjp0cnVlLCJ1cGRhdGUiOnRydWUsImRlbGV0ZSI6dHJ1ZSwiZW1haWwiOmZhbHNlLCJjYWxsIjpmYWxzZSwic21zIjpmYWxzZSwidGFzayI6ZmFsc2UsIm5vdGUiOmZhbHNlLCJyZWFkQWxsIjp0cnVlLCJ1cGRhdGVBbGwiOnRydWV9fV0sInVzZXJJZCI6IjEyIiwidXNlcm5hbWUiOiJ0b255QHN0YXJrLmNvbSIsInRlbmFudElkIjoiNTUifX0.xzQ-Ih5N1nllqkqgsBdS1NJgqhgNVJi1hiSZcuOrxp8";

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  public void givenWorkflowRequest_shouldCreate() throws IOException {
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
        getResourceAsString("/contracts/workflow/api/create-workflow-request.json");
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
  public void givenLeadWorkflowRequest_withCreateTaskAction_shouldCreate() throws IOException {
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
        getResourceAsString("/contracts/workflow/api/task/lead-create-workflow-with-create-task-action-request.json");
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
  @Sql("/test-scripts/integration/insert-lead-workflow-for-integration-test.sql")
  public void givenWorkflowUpdateRequest_shouldUpdate() throws IOException {

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
        getResourceAsString("/contracts/workflow/api/update-workflow-request.json");
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
        getResourceAsString("/contracts/workflow/api/integration/update-workflow-response.json");
    StepVerifier.create(workflowResponse)
        .assertNext(
            json -> {
              try {
                JSONAssert.assertEquals(
                    expectedResponse,
                    json,
                    new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("actions[0].id", (o1, o2) -> true),
                        new Customization("actions[0].type", (o1, o2) -> true),
                        new Customization("actions[1].id", (o1, o2) -> true),
                        new Customization("actions[2].id", (o1, o2) -> true),
                        new Customization("actions[3].id", (o1, o2) -> true),
                        new Customization("actions[0].payload", (o1, o2) -> true),
                        new Customization("actions[1].payload", (o1, o2) -> true),
                        new Customization("actions[2].payload", (o1, o2) -> true),
                        new Customization("actions[3].payload", (o1, o2) -> true),
                        new Customization("actions[3].type", (o1, o2) -> true),
                        new Customization("lastTriggeredAt", (o1, o2) -> true),
                        new Customization("updatedAt", (o1, o2) -> true),
                        new Customization("createdAt", (o1, o2) -> true),
                        new Customization("actions[1].type", (o1, o2) -> true),
                        new Customization("actions[2].type", (o1, o2) -> true)));
              } catch (JSONException e) {
                fail(e.getMessage());
              }
            })
        .verifyComplete();
  }

  @Test
  @Sql("/test-scripts/integration/insert-lead-workflow-for-integration-test.sql")
  public void givenLeadWorkflowUpdateRequest_withNewTaskAction_shouldUpdateWorkflow() throws IOException {

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
        get("/iam/v1/users/5")
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
        getResourceAsString("/contracts/workflow/api/task/lead-update-workflow-with-create-task-action-request.json");
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
        getResourceAsString("/contracts/workflow/api/task/lead-update-workflow-with-create-task-action-response.json");
    StepVerifier.create(workflowResponse)
        .assertNext(
            json -> {
              try {
                JSONAssert.assertEquals(
                    expectedResponse,
                    json,
                    new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("actions[0].id", (o1, o2) -> true),
                        new Customization("actions[0].type", (o1, o2) -> true),
                        new Customization("actions[1].id", (o1, o2) -> true),
                        new Customization("actions[2].id", (o1, o2) -> true),
                        new Customization("actions[3].id", (o1, o2) -> true),
                        new Customization("actions[0].payload", (o1, o2) -> true),
                        new Customization("actions[1].payload", (o1, o2) -> true),
                        new Customization("actions[2].payload", (o1, o2) -> true),
                        new Customization("actions[3].payload", (o1, o2) -> true),
                        new Customization("actions[3].type", (o1, o2) -> true),
                        new Customization("lastTriggeredAt", (o1, o2) -> true),
                        new Customization("updatedAt", (o1, o2) -> true),
                        new Customization("createdAt", (o1, o2) -> true),
                        new Customization("actions[1].type", (o1, o2) -> true),
                        new Customization("actions[2].type", (o1, o2) -> true)));
              } catch (JSONException e) {
                fail(e.getMessage());
              }
            })
        .verifyComplete();
  }

  @Test
  @Sql("/test-scripts/integration/insert-lead-workflow-for-integration-test.sql")
  public void givenWorkflowId_shouldGetIt() throws IOException {
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

    stubFor(
        get("/iam/v1/users/20003")
            .withHeader(AUTHORIZATION, matching("Bearer " + authenticationToken))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody(
                        getResourceAsString("/contracts/user/responses/user-details-by-id.json"))));
    // when
    var workflowResponse =
        buildWebClient().get().uri("/v1/workflows/301").retrieve().bodyToMono(String.class);
    // then
    var expectedResponse =
        getResourceAsString("/contracts/workflow/api/integration/workflowId-301-response.json");
    StepVerifier.create(workflowResponse)
        .assertNext(
            json -> {
              try {
                JSONAssert.assertEquals(
                    expectedResponse,
                    json,
                    new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("lastTriggeredAt", (o1, o2) -> true),
                        new Customization("createdAt", (o1, o2) -> true),
                        new Customization("updatedAt", (o1, o2) -> true),
                        new Customization("actions[0].id", (o1, o2) -> true),
                        new Customization("actions[1].id", (o1, o2) -> true),
                        new Customization("actions[0].type", (o1, o2) -> true),
                        new Customization("actions[1].type", (o1, o2) -> true),
                        new Customization("actions[0].payload", (o1, o2) -> true),
                        new Customization("actions[1].payload", (o1, o2) -> true)));
              } catch (JSONException e) {
                fail(e.getMessage());
              }
            })
        .verifyComplete();
  }

  @Test
  @Sql("/test-scripts/integration/workflow-with-condition-expression.sql")
  public void givenWorkflowId_withConditionExpression_shouldGetIt() throws IOException, JSONException {
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
    // when
    var workflowResponse =
        buildWebClient().get().uri("/v1/workflows/301").retrieve().bodyToMono(String.class).block();
    // then
    var expectedResponse =
        getResourceAsString("/contracts/workflow/api/integration/workflow-with-condition-response.json");
    JSONAssert.assertEquals(expectedResponse, workflowResponse, JSONCompareMode.LENIENT);
  }

  @Test
  @Sql("/test-scripts/integration/lead-workflow-with-condition-expression-and-old-value-trigger.sql")
  public void givenLeadWorkflowId_withConditionExpressionAndOldValueTriggerType_shouldGetIt() throws IOException, JSONException {
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
    // when
    var workflowResponse =
        buildWebClient().get().uri("/v1/workflows/301").retrieve().bodyToMono(String.class).block();
    // then
    var expectedResponse =
        getResourceAsString("/contracts/workflow/api/integration/workflow-with-condition-old-value-response.json");
    JSONAssert.assertEquals(expectedResponse, workflowResponse, JSONCompareMode.LENIENT);
  }

  @Test
  @Sql("/test-scripts/integration/contact-workflow-with-condition-expression-and-old-value-trigger.sql")
  public void givenContactWorkflowId_withConditionExpressionAndOldValueTriggerType_shouldGetIt() throws IOException, JSONException {
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
    // when
    var workflowResponse =
        buildWebClient().get().uri("/v1/workflows/301").retrieve().bodyToMono(String.class).block();
    // then
    var expectedResponse =
        getResourceAsString("/contracts/workflow/api/integration/contact-workflow-with-condition-old-value-response.json");
    JSONAssert.assertEquals(expectedResponse, workflowResponse, JSONCompareMode.LENIENT);
  }


  @Test
  @Sql("/test-scripts/integration/lead-workflow-with-pipeline-edit-property.sql")
  public void givenWorkflowId_havingPipelineEditProperty_shouldGetIt() throws IOException {
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

    stubFor(
            get("/search/v1/summaries/pipeline?id=1")
            .withHeader(AUTHORIZATION, matching("Bearer " + authenticationToken))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody(
                        getResourceAsString("/contracts/pipeline/responses/pipeline-details.json"))));
    // when
    var workflowResponse =
        buildWebClient().get().uri("/v1/workflows/301").retrieve().bodyToMono(String.class);
    // then
    var expectedResponse =
        getResourceAsString("/contracts/workflow/api/integration/workflowId-301-with-pipeline-response.json");
    StepVerifier.create(workflowResponse)
        .assertNext(
            json -> {
              try {
                JSONAssert.assertEquals(
                    expectedResponse,
                    json,
                    new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("lastTriggeredAt", (o1, o2) -> true),
                        new Customization("createdAt", (o1, o2) -> true),
                        new Customization("updatedAt", (o1, o2) -> true),
                        new Customization("actions[0].id", (o1, o2) -> true),
                        new Customization("actions[0].payload", (o1, o2) -> true)));
              } catch (JSONException e) {
                fail(e.getMessage());
              }
            })
        .verifyComplete();
  }

    @Test
    @Sql("/test-scripts/integration/lead-workflow-with-product-edit-property.sql")
    public void givenWorkflowId_havingProductEditProperty_shouldGetIt() throws IOException {
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

        stubFor(
                get("/product/v1/products/100")
                        .withHeader(AUTHORIZATION, matching("Bearer " + authenticationToken))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withStatus(200)
                                        .withBody(
                                                getResourceAsString("/contracts/pipeline/responses/product-details.json"))));
        // when
        var workflowResponse =
                buildWebClient().get().uri("/v1/workflows/302").retrieve().bodyToMono(String.class);
        // then
        var expectedResponse =
                getResourceAsString("/contracts/workflow/api/integration/workflowId-301-with-product-response.json");
        StepVerifier.create(workflowResponse)
                .assertNext(
                        json -> {
                            try {
                                JSONAssert.assertEquals(
                                        expectedResponse,
                                        json,
                                        new CustomComparator(
                                                JSONCompareMode.STRICT,
                                                new Customization("lastTriggeredAt", (o1, o2) -> true),
                                                new Customization("createdAt", (o1, o2) -> true),
                                                new Customization("updatedAt", (o1, o2) -> true),
                                                new Customization("actions[0].id", (o1, o2) -> true),
                                                new Customization("actions[0].payload", (o1, o2) -> true)));
                            } catch (JSONException e) {
                                fail(e.getMessage());
                            }
                        })
                .verifyComplete();
    }

  @Test
  @Sql("/test-scripts/integration/custom-param-webhook-workflow.sql")
  public void givenWorkflowId_withCustomWebhookParam_shouldGetIt() throws IOException {
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
    // when
    var workflowResponse =
        buildWebClient().get().uri("/v1/workflows/301").retrieve().bodyToMono(String.class);
    // then
    var expectedResponse =
        getResourceAsString("/contracts/workflow/api/integration/custom-param-response.json");
    StepVerifier.create(workflowResponse)
        .assertNext(
            json -> {
              try {
                JSONAssert.assertEquals(expectedResponse, json, JSONCompareMode.LENIENT);
              } catch (JSONException e) {
                fail(e.getMessage());
              }
            })
        .verifyComplete();
  }

  @Test
  @Sql("/test-scripts/integration/insert-lead-workflow-for-integration-test.sql")
  public void givenSearchRequest_tryToSortOnLastTriggeredAt_shouldSortAndGet() throws IOException {
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

    stubFor(
        get("/iam/v1/users/20003")
            .withHeader(AUTHORIZATION, matching("Bearer " + authenticationToken))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody(
                        getResourceAsString("/contracts/user/responses/user-details-by-id.json"))));
    // when
    var workflowResponse =
        buildWebClient()
            .post()
            .uri("/v1/workflows/search?page=0&size=10&sort=lastTriggeredAt,desc")
            .contentType(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(String.class);
    // then
    var expectedResponse =
        getResourceAsString(
            "/contracts/workflow/api/integration/search-sort-on-lastTriggeredAt.json");
    StepVerifier.create(workflowResponse)
        .assertNext(
            json -> {
              try {
                JSONAssert.assertEquals(
                    expectedResponse,
                    json,
                    new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("content[0].actions[0].id", (o1, o2) -> true),
                        new Customization("content[0].lastTriggeredAt", (o1, o2) -> true),
                        new Customization("content[0].createdAt", (o1, o2) -> true),
                        new Customization("content[0].updatedAt", (o1, o2) -> true),
                        new Customization("content[1].lastTriggeredAt", (o1, o2) -> true),
                        new Customization("content[1].createdAt", (o1, o2) -> true),
                        new Customization("content[1].updatedAt", (o1, o2) -> true),
                        new Customization("content[0].actions[1].id", (o1, o2) -> true),
                        new Customization("content[0].actions[0].type", (o1, o2) -> true),
                        new Customization("content[0].actions[1].type", (o1, o2) -> true),
                        new Customization("content[1].actions[0].payload", (o1, o2) -> true),
                        new Customization("content[0].actions[0].payload", (o1, o2) -> true),
                        new Customization("content[0].actions[1].payload", (o1, o2) -> true),
                        new Customization("content[1].actions[0].type", (o1, o2) -> true),
                        new Customization("content[1].actions[1].type", (o1, o2) -> true)));
              } catch (JSONException e) {
                fail(e.getMessage());
              }
            })
        .verifyComplete();
  }

  @Test
  public void getWebhookConfigurations_shouldFetchIt() throws IOException, JSONException {
    stubFor(
        get("/config/v1/entities/user/fields")
            .withHeader(AUTHORIZATION, matching("Bearer " + authenticationToken))
            .willReturn(
                aResponse()
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                    .withStatus(200)
                    .withBody(getResourceAsString("/contracts/config/response/get-all-user-fields.json"))));

    stubFor(
        get("/config/v1/entities/lead/fields")
            .withHeader(AUTHORIZATION, matching("Bearer " + authenticationToken))
            .willReturn(
                aResponse()
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                    .withStatus(200)
                    .withBody(getResourceAsString("/contracts/config/response/get-all-lead-fields.json"))));

    stubFor(
        get("/config/v1/entities/contact/fields")
            .withHeader(AUTHORIZATION, matching("Bearer " + authenticationToken))
            .willReturn(
                aResponse()
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                    .withStatus(200)
                    .withBody(getResourceAsString("/contracts/config/response/get-all-contact-fields.json"))));
    //when
    String response = buildWebClient()
        .get()
        .uri("/v1/workflows/webhook/LEAD/config")
        .retrieve()
        .bodyToMono(String.class)
        .block();
    //then
    var expectedResponse =
        getResourceAsString("/contracts/workflow/api/integration/webhook-configuration.json");
    JSONAssert.assertEquals(expectedResponse, response, JSONCompareMode.STRICT);

  }

  @Test
  @Sql("/test-scripts/integration/insert-lead-workflow-to-search.sql")
  public void givenSearchRequest_tryToFilter_shouldGet() throws IOException {
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
    // when
    var workflowResponse =
        buildWebClient()
            .post()
            .uri("/v1/workflows/search?page=0&size=10&sort=lastTriggeredAt,desc")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(getResourceAsString("/contracts/workflow/api/integration/search-workflow-apply-filter-request.json"))
            .retrieve()
            .bodyToMono(String.class);
    // then
    var expectedResponse =
        getResourceAsString(
            "contracts/workflow/api/integration/search-workflow-apply-filter-response.json");
    StepVerifier.create(workflowResponse)
        .assertNext(
            json -> {
              try {
                JSONAssert.assertEquals(
                    expectedResponse,
                    json,
                    new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("content[0].actions[0].id", (o1, o2) -> true),
                        new Customization("content[0].lastTriggeredAt", (o1, o2) -> true),
                        new Customization("content[0].createdAt", (o1, o2) -> true),
                        new Customization("content[0].updatedAt", (o1, o2) -> true),
                        new Customization("content[1].lastTriggeredAt", (o1, o2) -> true),
                        new Customization("content[1].createdAt", (o1, o2) -> true),
                        new Customization("content[1].updatedAt", (o1, o2) -> true)));
              } catch (JSONException e) {
                fail(e.getMessage());
              }
            })
        .verifyComplete();
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
