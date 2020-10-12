package com.kylas.sales.workflow.api;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;

import com.kylas.sales.workflow.api.request.WorkflowRequest;
import com.kylas.sales.workflow.api.response.WorkflowSummary;
import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import com.kylas.sales.workflow.domain.exception.InsufficientPrivilegeException;
import com.kylas.sales.workflow.domain.exception.InvalidActionException;
import com.kylas.sales.workflow.domain.workflow.ConditionType;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.TriggerFrequency;
import com.kylas.sales.workflow.domain.workflow.TriggerType;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(initializers = TestDatabaseInitializer.class)
class WorkflowControllerTest {

  @Autowired
  Environment environment;
  @Autowired
  MockMvc mockMvc;
  @Autowired
  ResourceLoader resourceLoader;
  @MockBean
  WorkflowService workflowService;

  private WebClient buildWebClient() {
    var port = environment.getProperty("local.server.port");

    return WebClient.builder()
        .baseUrl("http://localhost:" + port)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(
            HttpHeaders.AUTHORIZATION,
            "Bearer "
                + "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZWxsIiwiZGF0YSI6eyJleHBpcmVzSW4iOjQzMTk5LCJleHBpcnkiOjE1NzY0OTM3MTAsInRva2VuVHlwZSI6ImJlYXJlciIsInBlcm1pc3Npb25zIjpbeyJpZCI6NCwibmFtZSI6ImxlYWQiLCJkZXNjcmlwdGlvbiI6ImhhcyBhY2Nlc3MgdG8gbGVhZCByZXNvdXJjZSIsImxpbWl0cyI6LTEsInVuaXRzIjoiY291bnQiLCJhY3Rpb24iOnsicmVhZCI6dHJ1ZSwid3JpdGUiOnRydWUsInVwZGF0ZSI6dHJ1ZSwiZGVsZXRlIjp0cnVlLCJlbWFpbCI6ZmFsc2UsImNhbGwiOmZhbHNlLCJzbXMiOmZhbHNlLCJ0YXNrIjp0cnVlLCJub3RlIjp0cnVlLCJyZWFkQWxsIjp0cnVlLCJ1cGRhdGVBbGwiOnRydWV9fSx7ImlkIjo3LCJuYW1lIjoicHJvZHVjdHMtc2VydmljZXMiLCJkZXNjcmlwdGlvbiI6ImhhcyBhY2Nlc3MgdG8gdGVhbSByZXNvdXJjZSIsImxpbWl0cyI6LTEsInVuaXRzIjoiY291bnQiLCJhY3Rpb24iOnsicmVhZCI6dHJ1ZSwid3JpdGUiOnRydWUsInVwZGF0ZSI6dHJ1ZSwiZGVsZXRlIjp0cnVlLCJlbWFpbCI6ZmFsc2UsImNhbGwiOmZhbHNlLCJzbXMiOmZhbHNlLCJ0YXNrIjpmYWxzZSwibm90ZSI6ZmFsc2UsInJlYWRBbGwiOmZhbHNlLCJ1cGRhdGVBbGwiOmZhbHNlfX1dLCJ1c2VySWQiOiIxMiIsInVzZXJuYW1lIjoidG9ueUBzdGFyay5jb20iLCJ0ZW5hbnRJZCI6IjU1In19.fcqC0tgtZBzxpU5Si1IT8eOi4CNMckmPnVTze2xfmIk")
        .build();
  }

  @Test
  public void givenWorkflow_shouldCreateIt() {
    //given
    var requestPayload = getResourceAsString("classpath:contracts/workflow/api/create-workflow-request.json");
    given(workflowService.create(argThat(workflowRequest ->
        workflowRequest.getName().equalsIgnoreCase("Workflow 1")
            && workflowRequest.getDescription().equalsIgnoreCase("Workflow Description")
            && workflowRequest.getEntityType().equals(EntityType.LEAD)
            && workflowRequest.getTrigger().getName().equals(TriggerType.EVENT)
            && workflowRequest.getTrigger().getTriggerFrequency().equals(TriggerFrequency.CREATED)
            && workflowRequest.getCondition().getConditionType().equals(ConditionType.FOR_ALL)
            && workflowRequest.getActions().iterator().next().getType().equals(ActionType.EDIT_PROPERTY)
            && workflowRequest.getActions().iterator().next().getPayload().getName().equalsIgnoreCase("city")
            && workflowRequest.getActions().iterator().next().getPayload().getValue().equalsIgnoreCase("Pune")
    ))).willReturn(Mono.just(new WorkflowSummary(1L)));
    //when
    var workflowResponse = buildWebClient()
        .post()
        .uri("/v1/workflows")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requestPayload)
        .retrieve()
        .bodyToMono(String.class);
    //then
    var expectedResponse =
        getResourceAsString("classpath:contracts/workflow/api/create-workflow-response.json");
    StepVerifier.create(workflowResponse)
        .assertNext(json -> {
          try {
            JSONAssert.assertEquals(expectedResponse, json, false);
          } catch (JSONException e) {
            fail(e.getMessage());
          }
        }).verifyComplete();
  }

  @Test
  public void givenActionWithoutNameWorkflow_shouldThrow() {
    //given
    var requestPayload = getResourceAsString("classpath:contracts/workflow/api/create-workflow-request.json");

    given(workflowService.create(any(WorkflowRequest.class))).willThrow(new InvalidActionException());
    //when
    var workflowResponse = buildWebClient()
        .post()
        .uri("/v1/workflows")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requestPayload)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .block().bodyToMono(String.class);

    StepVerifier.create(workflowResponse)
        .assertNext(json -> {
          try {
            JSONAssert.assertEquals("{\"code\":\"01701002\"}", json, false);
          } catch (JSONException e) {
            fail(e.getMessage());
          }
        }).verifyComplete();

  }

  @Test
  public void shouldReturnValidResponseForInsufficientPrivilegeException() {
    //given
    var requestPayload = getResourceAsString("classpath:contracts/workflow/api/create-workflow-request.json");

    given(workflowService.create(any(WorkflowRequest.class))).willThrow(new InsufficientPrivilegeException());
    //when
    var workflowResponse = buildWebClient()
        .post()
        .uri("/v1/workflows")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requestPayload)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .block().bodyToMono(String.class);

    StepVerifier.create(workflowResponse)
        .assertNext(json -> {
          try {
            JSONAssert.assertEquals("{\"code\":\"01702001\"}", json, false);
          } catch (JSONException e) {
            fail(e.getMessage());
          }
        }).verifyComplete();

  }

  private String getResourceAsString(String resourcePath) {
    var resource = resourceLoader.getResource(resourcePath);
    File file = null;
    try {
      file = resource.getFile();
      return FileUtils.readFileToString(file, "UTF-8");
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }
}