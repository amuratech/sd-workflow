package com.kylas.sales.workflow.api;

import static com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.ValueType.ARRAY;
import static com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.ValueType.OBJECT;
import static com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.ValueType.PLAIN;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.kylas.sales.workflow.api.request.Condition;
import com.kylas.sales.workflow.api.request.Condition.ExpressionElement;
import com.kylas.sales.workflow.api.request.FilterRequest;
import com.kylas.sales.workflow.api.request.FilterRequest.Filter;
import com.kylas.sales.workflow.api.request.WorkflowRequest;
import com.kylas.sales.workflow.api.response.WorkflowDetail;
import com.kylas.sales.workflow.api.response.WorkflowEntry;
import com.kylas.sales.workflow.api.response.WorkflowSummary;
import com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction;
import com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.ValueType;
import com.kylas.sales.workflow.common.dto.ActionDetail.ReassignAction;
import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.common.dto.User;
import com.kylas.sales.workflow.common.dto.WorkflowTrigger;
import com.kylas.sales.workflow.common.dto.condition.Operator;
import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import com.kylas.sales.workflow.domain.exception.InsufficientPrivilegeException;
import com.kylas.sales.workflow.domain.exception.InvalidActionException;
import com.kylas.sales.workflow.domain.exception.InvalidFilterException;
import com.kylas.sales.workflow.domain.exception.WorkflowNotFoundException;
import com.kylas.sales.workflow.domain.workflow.ConditionType;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.TriggerFrequency;
import com.kylas.sales.workflow.domain.workflow.TriggerType;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType;
import com.kylas.sales.workflow.matchers.FilterRequestMatcher;
import com.kylas.sales.workflow.matchers.PageableMatcher;
import com.kylas.sales.workflow.stubs.WorkflowStub;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
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
  public void givenWorkflow_withDifferentValueTypes_shouldCreateIt() throws IOException {
    //given
    var requestPayload = getResourceAsString("classpath:contracts/workflow/api/create-workflow-request.json");
    given(workflowService.create(argThat(workflowRequest ->
        {
          List<ValueType> mockList = List.of(ARRAY, PLAIN);
          List<ValueType> valueTypeList = new ArrayList<>();
          workflowRequest.getActions().stream().filter(action -> action.getType().equals(ActionType.EDIT_PROPERTY)).forEach(action -> {
            EditPropertyAction payload = (EditPropertyAction) action.getPayload();
            valueTypeList.add(payload.getValueType());
          });
          return workflowRequest.getName().equalsIgnoreCase("Workflow 1")
              && workflowRequest.getDescription().equalsIgnoreCase("Workflow Description")
              && workflowRequest.getEntityType().equals(EntityType.LEAD)
              && workflowRequest.getTrigger().getName().equals(TriggerType.EVENT)
              && workflowRequest.getTrigger().getTriggerFrequency().equals(TriggerFrequency.CREATED)
              && workflowRequest.getCondition().getConditionType().equals(ConditionType.FOR_ALL)
              && valueTypeList.containsAll(mockList)
              && workflowRequest.getActions().size() == 34
              && workflowRequest.isActive();
        }
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
  public void givenWorkflowWithMultipleActions_andDifferentValueTypes_shouldCreateIt() throws IOException {
    //given
    var requestPayload = getResourceAsString("classpath:contracts/workflow/api/create-multiple-actions-workflow.json");
    given(workflowService.create(argThat(
        workflowRequest -> {
          List<ValueType> mockList = List.of(ARRAY, PLAIN);
          List<ValueType> valueTypeList = new ArrayList<>();
          workflowRequest.getActions().stream().filter(action -> action.getType().equals(ActionType.EDIT_PROPERTY)).forEach(action -> {
            EditPropertyAction payload = (EditPropertyAction) action.getPayload();
            valueTypeList.add(payload.getValueType());
          });
          return workflowRequest.getActions()
              .stream()
              .anyMatch(webhookAction ->
                  webhookAction.getType().equals(ActionType.WEBHOOK))
              && workflowRequest.getActions()
              .stream()
              .anyMatch(webhookAction ->
                  webhookAction.getType().equals(ActionType.REASSIGN))
              && valueTypeList.size() == 33
              && valueTypeList.containsAll(mockList);
        }
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
  public void givenWorkflowUpdate_withDifferentValueTypes_shouldUpdateIt() throws IOException {
    //given
    var requestPayload = getResourceAsString("classpath:contracts/workflow/api/update-workflow-request.json");
    ObjectMapper objectMapper = new ObjectMapper();
    WorkflowTrigger trigger = new WorkflowTrigger(TriggerType.EVENT, TriggerFrequency.CREATED);

    List<ActionResponse> actions =
        List.of(
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("salutation", 1319, PLAIN)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("dnd", true, PLAIN)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("city", "PUNE", PLAIN)),
            new ActionResponse(ActionType.REASSIGN, new ReassignAction(20003L, "Tony Stark")));
    Condition condition = new Condition(ConditionType.FOR_ALL.name(), null);
    User user = new User(5000L, "Tony Stark");
    WorkflowDetail workflowDetail = new WorkflowDetail(1L, "Workflow 1", "Workflow Description", EntityType.LEAD, trigger, condition, actions, user,
        user, null, null, null, 0L, null, true);
    given(workflowService.update(eq(1L), argThat(workflowRequest ->
        {
          List<ValueType> mockList = List.of(PLAIN);
          List<ValueType> valueTypeList = new ArrayList<>();
          workflowRequest.getActions().stream().filter(action -> action.getType().equals(ActionType.EDIT_PROPERTY)).forEach(action -> {
            EditPropertyAction payload = (EditPropertyAction) action.getPayload();
            valueTypeList.add(payload.getValueType());
          });
          return workflowRequest.getName().equalsIgnoreCase("Workflow 1")
              && workflowRequest.getDescription().equalsIgnoreCase("Workflow Description")
              && workflowRequest.getEntityType().equals(EntityType.LEAD)
              && workflowRequest.getTrigger().getName().equals(TriggerType.EVENT)
              && workflowRequest.getTrigger().getTriggerFrequency().equals(TriggerFrequency.CREATED)
              && workflowRequest.getCondition().getConditionType().equals(ConditionType.FOR_ALL)
              && valueTypeList.size() == 3
              && valueTypeList.containsAll(mockList)
              && workflowRequest.isActive();
        }
    ))).willReturn(Mono.just(workflowDetail));
    //when
    var workflowResponse = buildWebClient()
        .put()
        .uri("/v1/workflows/1")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requestPayload)
        .retrieve()
        .bodyToMono(String.class);
    //then
    var expectedResponse =
        getResourceAsString("classpath:contracts/workflow/api/update-workflow-response.json");
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
  public void givenDeactivatedWorkflow_shouldCreateIt() throws IOException {
    //given
    var requestPayload = getResourceAsString("classpath:contracts/workflow/api/create-deactivated-workflow-request.json");
    given(workflowService.create(argThat(workflowRequest ->
        {
          List<ValueType> mockList = List.of(ARRAY, PLAIN);
          List<ValueType> valueTypeList = new ArrayList<>();
          workflowRequest.getActions().forEach(action -> {
            EditPropertyAction payload = (EditPropertyAction) action.getPayload();
            valueTypeList.add(payload.getValueType());
          });
          return workflowRequest.getName().equalsIgnoreCase("Workflow 1")
              && workflowRequest.getDescription().equalsIgnoreCase("Workflow Description")
              && workflowRequest.getEntityType().equals(EntityType.LEAD)
              && workflowRequest.getTrigger().getName().equals(TriggerType.EVENT)
              && workflowRequest.getTrigger().getTriggerFrequency().equals(TriggerFrequency.CREATED)
              && workflowRequest.getCondition().getConditionType().equals(ConditionType.FOR_ALL)
              && workflowRequest.getActions().stream().allMatch(action -> action.getType().equals(ActionType.EDIT_PROPERTY)
              && valueTypeList.containsAll(mockList)
              && workflowRequest.getActions().size() == 33
              && !workflowRequest.isActive());
        }
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
  public void givenActionWithoutNameWorkflow_shouldThrow() throws IOException {
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
  public void shouldReturnValidResponseForInsufficientPrivilegeException() throws IOException {
    // given
    var requestPayload =
        getResourceAsString("classpath:contracts/workflow/api/create-workflow-request.json");

    given(workflowService.create(any(WorkflowRequest.class)))
        .willThrow(new InsufficientPrivilegeException());
    // when
    var workflowResponse =
        buildWebClient()
            .post()
            .uri("/v1/workflows")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestPayload)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .block()
            .bodyToMono(String.class);

    StepVerifier.create(workflowResponse)
        .assertNext(
            json -> {
              try {
                JSONAssert.assertEquals("{\"code\":\"01702001\"}", json, false);
              } catch (JSONException e) {
                fail(e.getMessage());
              }
            })
        .verifyComplete();
  }

  @Test
  public void givenWorkflowId_shouldGetIt() throws IOException {
    // given
    long workflowId = 101L;

    User createdBy = new User(101L, "Tony Start");
    var updatedBy = new User(102L, "Steve Roger");

    WorkflowDetail workflowDetail =
        WorkflowStub.workflowDetail(
            workflowId,
            "Edit Lead Property",
            "Edit Lead Property",
            EntityType.LEAD,
            true,
            TriggerType.EVENT,
            TriggerFrequency.CREATED,
            ConditionType.FOR_ALL,
            ActionType.EDIT_PROPERTY,
            "lastName",
            "Stark",
            PLAIN,
            true,
            true,
            createdBy,
            updatedBy,
            new Date());
    given(workflowService.get(workflowId)).willReturn(Mono.just(workflowDetail));
    // when
    var workflowResponse =
        buildWebClient()
            .get()
            .uri("/v1/workflows/" + workflowId)
            .retrieve()
            .bodyToMono(String.class);
    // then
    var expectedResponse =
        getResourceAsString("classpath:contracts/workflow/api/get-workflow-response.json");
    StepVerifier.create(workflowResponse)
        .assertNext(
            json -> {
              try {
                JSONAssert.assertEquals(
                    expectedResponse,
                    json,
                    new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("createdAt", (o1, o2) -> true),
                        new Customization("updatedAt", (o1, o2) -> true),
                        new Customization("actions[0].id", (o1, o2) -> true)));
              } catch (JSONException e) {
                fail(e.getMessage());
              }
            })
        .verifyComplete();
  }

  @Test
  public void givenWorkflowId_shouldActivateIt() throws IOException {
    // given
    long workflowId = 101L;
    User createdBy = new User(101L, "Tony Start");
    var updatedBy = new User(102L, "Steve Roger");

    WorkflowDetail workflowDetail =
        WorkflowStub.workflowDetail(
            workflowId,
            "Edit Lead Property",
            "Edit Lead Property",
            EntityType.LEAD,
            true,
            TriggerType.EVENT,
            TriggerFrequency.CREATED,
            ConditionType.FOR_ALL,
            ActionType.EDIT_PROPERTY,
            "lastName",
            "Stark",
            PLAIN,
            true,
            true,
            createdBy,
            updatedBy,
            new Date());
    given(workflowService.activate(workflowId)).willReturn(Mono.just(workflowDetail));
    // when
    var workflowResponse =
        buildWebClient()
            .post()
            .uri("/v1/workflows/101/activate")
            .retrieve()
            .bodyToMono(String.class);
    // then
    var expectedResponse =
        getResourceAsString("classpath:contracts/workflow/api/activate-workflow-response.json");
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
  public void givenWorkflowId_shouldDeactivateIt() throws IOException {
    // given
    long workflowId = 101L;
    User createdBy = new User(101L, "Tony Start");
    var updatedBy = new User(102L, "Steve Roger");

    WorkflowDetail workflowDetail =
        WorkflowStub.workflowDetail(
            workflowId,
            "Edit Lead Property",
            "Edit Lead Property",
            EntityType.LEAD,
            false,
            TriggerType.EVENT,
            TriggerFrequency.CREATED,
            ConditionType.FOR_ALL,
            ActionType.EDIT_PROPERTY,
            "lastName",
            "Stark",
            PLAIN,
            true,
            true,
            createdBy,
            updatedBy,
            new Date());
    given(workflowService.deactivate(workflowId)).willReturn(Mono.just(workflowDetail));
    // when
    var workflowResponse =
        buildWebClient()
            .post()
            .uri("/v1/workflows/101/deactivate")
            .retrieve()
            .bodyToMono(String.class);
    // then
    var expectedResponse =
        getResourceAsString("classpath:contracts/workflow/api/deactivate-workflow-response.json");
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
  public void givenNonExitWorkflowId_shouldThrow() {
    // given
    long workflowId = 101L;
    given(workflowService.get(workflowId)).willThrow(new WorkflowNotFoundException());
    // when
    var workflowResponse =
        buildWebClient()
            .get()
            .uri("/v1/workflows/" + workflowId)
            .exchange()
            .block()
            .bodyToMono(String.class);
    // then
    StepVerifier.create(workflowResponse)
        .assertNext(
            json -> {
              try {
                JSONAssert.assertEquals("{\"code\":\"01701005\"}", json, false);
              } catch (JSONException e) {
                fail(e.getMessage());
              }
            })
        .verifyComplete();
  }

  @Test
  public void givenListRequest_shouldGetPageableListingPage() throws IOException {
    // given

    User createdBy = new User(101L, "Tony Start");
    var updatedBy = new User(102L, "Steve Roger");

    WorkflowDetail workflowDetail1 =
        WorkflowStub.workflowDetail(
            101,
            "Workflow 1",
            "Workflow 1",
            EntityType.LEAD,
            true,
            TriggerType.EVENT,
            TriggerFrequency.CREATED,
            ConditionType.FOR_ALL,
            ActionType.EDIT_PROPERTY,
            "lastName",
            "Stark",
            PLAIN,
            true,
            true,
            createdBy,
            updatedBy,
            new Date());

    WorkflowDetail workflowDetail2 =
        WorkflowStub.workflowDetail(
            102,
            "Workflow 2",
            "Workflow 2",
            EntityType.LEAD,
            true,
            TriggerType.EVENT,
            TriggerFrequency.CREATED,
            ConditionType.FOR_ALL,
            ActionType.EDIT_PROPERTY,
            "firstName",
            "Tony",
            PLAIN,
            true,
            true,
            createdBy,
            updatedBy,
            new Date());
    given(workflowService.list(argThat(new PageableMatcher(0, 10, Sort.unsorted()))))
        .willReturn(
            Mono.just(
                new PageImpl<>(
                    asList(workflowDetail1, workflowDetail2), PageRequest.of(0, 10), 12)));
    // when
    var workflowResponse =
        buildWebClient()
            .post()
            .uri("/v1/workflows/list?page=0&size=10")
            .contentType(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(String.class);
    // then
    var expectedResponse =
        getResourceAsString("classpath:contracts/workflow/api/list-workflows.json");
    StepVerifier.create(workflowResponse)
        .assertNext(
            json -> {
              try {
                JSONAssert.assertEquals(
                    expectedResponse,
                    json,
                    new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("content[0].createdAt", (o1, o2) -> true),
                        new Customization("content[0].updatedAt", (o1, o2) -> true),
                        new Customization("content[0].actions[0].id", (o1, o2) -> true),
                        new Customization("content[1].createdAt", (o1, o2) -> true),
                        new Customization("content[1].updatedAt", (o1, o2) -> true),
                        new Customization("content[1].actions[0].id", (o1, o2) -> true)));
              } catch (JSONException e) {
                fail(e.getMessage());
              }
            })
        .expectComplete()
        .verify();
  }

  @Test
  public void givenSearchRequest_tryToSortOnLastTriggeredAt_shouldGetPageableListingPage() throws IOException {
    // given

    User createdBy = new User(101L, "Tony Start");
    var updatedBy = new User(102L, "Steve Roger");

    WorkflowEntry workflowDetail1 =
        WorkflowStub.workflowEntry(
            101,
            "Workflow 1",
            EntityType.LEAD,
            true,
            true,
            true,
            createdBy,
            updatedBy,
            new Date());

    WorkflowEntry workflowDetail2 =
        WorkflowStub.workflowEntry(
            102,
            "Workflow 2",
            EntityType.LEAD,
            true,
            true,
            true,
            createdBy,
            updatedBy,
            new Date());

    Sort sortByLastTriggeredAt = Sort.by(Order.desc("lastTriggeredAt"));
    Sort expectedSortable = Sort.by(Order.desc("workflowExecutedEvent.lastTriggeredAt"));

    given(
        workflowService.search(
            argThat(new PageableMatcher(0, 10, expectedSortable)),
            argThat(Optional::isEmpty)))
        .willReturn(
            new PageImpl<>(
                asList(workflowDetail1, workflowDetail2),
                PageRequest.of(0, 10, sortByLastTriggeredAt),
                12));
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
            "classpath:contracts/workflow/api/search-workflows-sort-on-lastTriggeredAt-desc.json");
    StepVerifier.create(workflowResponse)
        .assertNext(
            json -> {
              try {
                JSONAssert.assertEquals(
                    expectedResponse,
                    json,
                    new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("content[0].createdAt", (o1, o2) -> true),
                        new Customization("content[0].updatedAt", (o1, o2) -> true),
                        new Customization("content[0].actions[0].id", (o1, o2) -> true),
                        new Customization("content[1].createdAt", (o1, o2) -> true),
                        new Customization("content[1].updatedAt", (o1, o2) -> true),
                        new Customization("content[1].actions[0].id", (o1, o2) -> true)));
              } catch (JSONException e) {
                fail(e.getMessage());
              }
            })
        .expectComplete()
        .verify();
  }

  @Test
  public void givenSearchRequest_tryToSortOnLastUpdatedAt_shouldGetPageableListingPage() throws IOException {
    // given

    User createdBy = new User(101L, "Tony Start");
    var updatedBy = new User(102L, "Steve Roger");

    WorkflowEntry workflowDetail1 =
        WorkflowStub.workflowEntry(
            101,
            "Workflow 1",
            EntityType.LEAD,
            true,
            true,
            true,
            createdBy,
            updatedBy,
            new Date());

    WorkflowEntry workflowDetail2 =
        WorkflowStub.workflowEntry(
            102,
            "Workflow 2",
            EntityType.LEAD,
            true,
            true,
            true,
            createdBy,
            updatedBy,
            new Date());

    Sort sortByLastTriggeredAt = Sort.by(Order.desc("lastUpdatedAt"));

    given(
        workflowService.search(
            argThat(new PageableMatcher(0, 10, sortByLastTriggeredAt)),
            argThat(filterRequest -> filterRequest.get().getFilters().isEmpty())))
        .willReturn(
            new PageImpl<>(
                asList(workflowDetail1, workflowDetail2),
                PageRequest.of(0, 10, sortByLastTriggeredAt),
                12));
    // when
    var workflowResponse =
        buildWebClient()
            .post()
            .uri("/v1/workflows/search?page=0&size=10&sort=lastUpdatedAt,desc")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                "{\"fields\":[\"name\",\"entityType\",\"createdAt\",\"createdBy\",\"lastTriggeredAt\",\"triggerCount\"],\"jsonRules\":null}")
            .retrieve()
            .bodyToMono(String.class);
    // then
    var expectedResponse =
        getResourceAsString(
            "classpath:contracts/workflow/api/search-workflows-sort-on-lastUpdatedAt-desc.json");
    StepVerifier.create(workflowResponse)
        .assertNext(
            json -> {
              try {
                JSONAssert.assertEquals(
                    expectedResponse,
                    json,
                    new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("content[0].createdAt", (o1, o2) -> true),
                        new Customization("content[0].updatedAt", (o1, o2) -> true),
                        new Customization("content[0].actions[0].id", (o1, o2) -> true),
                        new Customization("content[1].createdAt", (o1, o2) -> true),
                        new Customization("content[1].updatedAt", (o1, o2) -> true),
                        new Customization("content[1].actions[0].id", (o1, o2) -> true)));
              } catch (JSONException e) {
                fail(e.getMessage());
              }
            })
        .expectComplete()
        .verify();
  }

  @Test
  public void givenFilterSearchRequest_shouldSearch() throws IOException {
    // given
    User createdBy = new User(101L, "Tony Start");
    var updatedBy = new User(102L, "Steve Roger");

    WorkflowEntry workflowDetail1 =
        WorkflowStub.workflowEntry(
            101,
            "Workflow 1",
            EntityType.LEAD,
            true,
            true,
            true,
            createdBy,
            updatedBy,
            new Date());

    WorkflowEntry workflowDetail2 =
        WorkflowStub.workflowEntry(
            102,
            "Workflow 2",
            EntityType.LEAD,
            true,
            true,
            true,
            createdBy,
            updatedBy,
            new Date());

    Sort sortByLastTriggeredAt = Sort.by(Order.desc("lastUpdatedAt"));
    List<Filter> filters = new ArrayList<>();
    filters.add(new Filter("equals", "entityType", "string", "LEAD"));
    FilterRequest expectedFilter = new FilterRequest(filters);
    given(
        workflowService.search(
            argThat(new PageableMatcher(0, 10, sortByLastTriggeredAt)),
            argThat(new FilterRequestMatcher(expectedFilter))))
        .willReturn(
            new PageImpl<>(
                asList(workflowDetail1, workflowDetail2),
                PageRequest.of(0, 10, sortByLastTriggeredAt),
                12));
    // when
    var workflowResponse =
        buildWebClient()
            .post()
            .uri("/v1/workflows/search?page=0&size=10&sort=lastUpdatedAt,desc")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                getResourceAsString(
                    "classpath:contracts/workflow/api/workflow-filter-request.json"))
            .retrieve()
            .bodyToMono(String.class);
    // then
    var expectedResponse =
        getResourceAsString(
            "classpath:contracts/workflow/api/search-workflows-sort-on-lastUpdatedAt-desc.json");
    StepVerifier.create(workflowResponse)
        .assertNext(
            json -> {
              try {
                JSONAssert.assertEquals(
                    expectedResponse,
                    json,
                    new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("content[0].createdAt", (o1, o2) -> true),
                        new Customization("content[0].updatedAt", (o1, o2) -> true),
                        new Customization("content[0].actions[0].id", (o1, o2) -> true),
                        new Customization("content[1].createdAt", (o1, o2) -> true),
                        new Customization("content[1].updatedAt", (o1, o2) -> true),
                        new Customization("content[1].actions[0].id", (o1, o2) -> true)));
              } catch (JSONException e) {
                fail(e.getMessage());
              }
            })
        .expectComplete()
        .verify();
  }


  @Test
  public void givenInvalidFilter_shouldThrow() throws IOException {
    // given
    User createdBy = new User(101L, "Tony Start");
    var updatedBy = new User(102L, "Steve Roger");

    Sort sortByLastTriggeredAt = Sort.by(Order.desc("lastUpdatedAt"));
    List<Filter> filters = new ArrayList<>();
    filters.add(new Filter("equals", "entityType", "string", "LEAD"));
    FilterRequest expectedFilter = new FilterRequest(filters);
    given(
        workflowService.search(any(), any())).willThrow(new InvalidFilterException());
    // when
    var workflowResponse =
        buildWebClient()
            .post()
            .uri("/v1/workflows/search?page=0&size=10&sort=lastUpdatedAt,desc")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                getResourceAsString(
                    "classpath:contracts/workflow/api/workflow-inValid-filter-request.json"))
            .exchange().block().bodyToMono(String.class);
    // then
    StepVerifier.create(workflowResponse)
        .assertNext(
            json -> {
              try {
                JSONAssert.assertEquals(
                    "{\"code\":\"01701006\"}",
                    json,
                    JSONCompareMode.LENIENT);
              } catch (JSONException e) {
                fail(e.getMessage());
              }
            })
        .expectComplete()
        .verify();
  }

  @Test
  public void givenWorkflow_withReassignAction_shouldCreateIt() throws IOException {
    //given
    var requestPayload = getResourceAsString("classpath:contracts/reassign/create-workflow-with-reassign-action-request.json");
    given(workflowService.create(argThat(workflowRequest -> {
      return workflowRequest.getActions().stream().allMatch(action -> action.getType().equals(ActionType.REASSIGN));
    }))).willReturn(Mono.just(new WorkflowSummary(1L)));

    //when
    var workflowResponse = buildWebClient()
        .post()
        .uri("/v1/workflows")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requestPayload).retrieve()
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
  public void givenWorkflow_withReassignAction_shouldUpdateIt() throws IOException {
    //given
    var requestPayload = getResourceAsString("classpath:contracts/reassign/update-workflow-with-reassign-action-request.json");
    WorkflowTrigger trigger = new WorkflowTrigger(TriggerType.EVENT, TriggerFrequency.CREATED);
    Condition condition = new Condition(ConditionType.FOR_ALL.name(), null);
    List<ActionResponse> actions =
        List.of(new ActionResponse(ActionType.REASSIGN, new ReassignAction(20003L, "Tony Stark")));
    User user = new User(5000L, "Tony Stark");
    WorkflowDetail workflowDetail = new WorkflowDetail(1L, "Workflow 1", "Workflow Description", EntityType.LEAD, trigger, condition, actions, user,
        user, null, null, null, 0L, null, true);
    given(workflowService.update(eq(1L), argThat(workflowRequest -> {
      return workflowRequest.getActions().stream().allMatch(action -> action.getType().equals(ActionType.REASSIGN));
    }))).willReturn(Mono.just(workflowDetail));

    //when
    var workflowResponse = buildWebClient()
        .put()
        .uri("/v1/workflows/1")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requestPayload)
        .retrieve()
        .bodyToMono(String.class);

    //then
    var expectedResponse =
        getResourceAsString("classpath:contracts/reassign/update-workflow-reassign-action-response.json");
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
  public void givenContactWorkflow_withReassignAction_shouldCreateIt() throws IOException {
    //given
    var requestPayload = getResourceAsString("classpath:contracts/reassign/create-contact-workflow-with-reassign-action-request.json");
    given(workflowService.create(argThat(workflowRequest -> {
      return workflowRequest.getActions().stream().allMatch(action -> action.getType().equals(ActionType.REASSIGN));
    }))).willReturn(Mono.just(new WorkflowSummary(1L)));

    //when
    var workflowResponse = buildWebClient()
        .post()
        .uri("/v1/workflows")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requestPayload).retrieve()
        .bodyToMono(String.class);

    //then
    var expectedResponse =
        getResourceAsString("classpath:contracts/workflow/api/create-contact-workflow-response.json");
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
  public void givenContactWorkflow_withReassignAction_shouldUpdateIt() throws IOException {
    //given
    var requestPayload = getResourceAsString("classpath:contracts/reassign/update-contact-workflow-with-reassign-action-request.json");
    WorkflowTrigger trigger = new WorkflowTrigger(TriggerType.EVENT, TriggerFrequency.CREATED);
    Condition condition = new Condition(ConditionType.FOR_ALL.name(), null);
    List<ActionResponse> actions =
        List.of(new ActionResponse(ActionType.REASSIGN, new ReassignAction(20003L, "Tony Stark")));
    User user = new User(5000L, "Tony Stark");
    WorkflowDetail workflowDetail = new WorkflowDetail(1L, "Workflow 1", "Workflow Description", EntityType.CONTACT, trigger, condition, actions,
        user,
        user, null, null, null, 0L, null, true);
    given(workflowService.update(eq(1L), argThat(workflowRequest -> {
      return workflowRequest.getActions().stream().allMatch(action -> action.getType().equals(ActionType.REASSIGN));
    }))).willReturn(Mono.just(workflowDetail));

    //when
    var workflowResponse = buildWebClient()
        .put()
        .uri("/v1/workflows/1")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requestPayload)
        .retrieve()
        .bodyToMono(String.class);

    //then
    var expectedResponse =
        getResourceAsString("classpath:contracts/reassign/update-contact-workflow-reassign-action-response.json");
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
  public void givenWorkflowRequest_withIdNameCondition_shouldCreateIt() throws JSONException, IOException {
    //given
    var requestPayload =
        getResourceAsString("classpath:contracts/workflow/api/workflow-request-with-idName-condition.json");
    given(workflowService.create(argThat(workflowRequest ->
        {
          ExpressionElement expressionElement = workflowRequest.getCondition().getConditions().get(0);
          return workflowRequest.getName().equalsIgnoreCase("Workflow 1")
              && workflowRequest.getDescription().equalsIgnoreCase("Workflow Description")
              && workflowRequest.getCondition().getConditionType().equals(ConditionType.CONDITION_BASED)
              && expressionElement.getName().equals("pipeline")
              && expressionElement.getOperator().equals(Operator.EQUAL)
              && ((LinkedHashMap) expressionElement.getValue()).get("id").equals(242);
        })
    )).willReturn(Mono.just(new WorkflowSummary(1L)));
    //when
    var workflowResponse = buildWebClient()
        .post()
        .uri("/v1/workflows")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requestPayload)
        .retrieve()
        .bodyToMono(String.class).block();
    //then
    var expectedResponse =
        getResourceAsString("classpath:contracts/workflow/api/create-workflow-response.json");
    JSONAssert.assertEquals(expectedResponse, workflowResponse, false);
  }

  @Test
  public void givenWorkflowRequest_withEntityContact_shouldCreateIt() throws IOException {
    //given
    var requestPayload = getResourceAsString("classpath:contracts/workflow/api/create-workflow-request-with-entity-contact.json");
    given(workflowService.create(argThat(workflowRequest ->
        {
          List<ValueType> mockList = List.of(ARRAY, PLAIN, OBJECT);
          List<ValueType> valueTypeList = new ArrayList<>();
          workflowRequest.getActions().stream().filter(action -> action.getType().equals(ActionType.EDIT_PROPERTY)).forEach(action -> {
            EditPropertyAction payload = (EditPropertyAction) action.getPayload();
            valueTypeList.add(payload.getValueType());
          });
          return workflowRequest.getName().equalsIgnoreCase("Workflow 1")
              && workflowRequest.getDescription().equalsIgnoreCase("Workflow Description")
              && workflowRequest.getEntityType().equals(EntityType.CONTACT)
              && workflowRequest.getTrigger().getName().equals(TriggerType.EVENT)
              && workflowRequest.getTrigger().getTriggerFrequency().equals(TriggerFrequency.CREATED)
              && workflowRequest.getCondition().getConditionType().equals(ConditionType.FOR_ALL)
              && workflowRequest.isActive();
        }
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
        getResourceAsString("classpath:contracts/workflow/api/create-workflow-response-with-entity-contact.json");
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
  public void givenWorkflowRequest_withEntityContact_shouldUpdateIt() throws IOException, JSONException {
    //given
    var requestPayload = getResourceAsString("classpath:contracts/workflow/api/update-workflow-request-with-entity-contact.json");
    WorkflowTrigger trigger = new WorkflowTrigger(TriggerType.EVENT, TriggerFrequency.CREATED);
    ObjectMapper objectMapper = new ObjectMapper();
    Condition condition = new Condition(ConditionType.FOR_ALL.name(), null);
    List<ActionResponse> actions =
        List.of(
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("salutation", 1319, PLAIN)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("dnd", true, PLAIN)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("timezone", "Asia/Calcutta", PLAIN)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("address", "pune rural", PLAIN)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("city", "PUNE", PLAIN)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("state", "Maharashtra", PLAIN)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("zipcode", "412410", PLAIN)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("companyName", "Kylas", PLAIN)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("country", "IN", PLAIN)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("facebook", "https://facebook.com/james", PLAIN)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("firstName", "James", PLAIN)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("lastName", "Bond", PLAIN)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("twitter", "https://twitter.com/james", PLAIN)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("linkedIn", "https://linkedin.com/james", PLAIN)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("department", "CBI", PLAIN)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("designation", "Inspector", PLAIN)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("stakeholder", true, PLAIN)),
            new ActionResponse(ActionType.EDIT_PROPERTY,
                new EditPropertyAction("company", objectMapper.readValue("{\"id\":201,\"name\":\"Uflex\"}", Object.class), OBJECT)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("associatedDeals",
                objectMapper.readValue("[{\"id\":100,\"name\":\"BestDeal\"}]", Object[].class), ARRAY)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("phoneNumbers",
                objectMapper
                    .readValue("[{\"type\":\"HOME\",\"code\":\"0253\",\"value\":\"+0253\",\"dialCode\":\"2459817\",\"isPrimary\":true}]",
                        Object[].class),
                ARRAY)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("emails",
                objectMapper.readValue("[{\"type\":\"OFFICE\",\"value\":\"john150@outlook.com\",\"isPrimary\":true}]", Object[].class), ARRAY)));

    User user = new User(5000L, "Tony Stark");
    WorkflowDetail workflowDetail = new WorkflowDetail(1L, "Workflow 1", "Workflow Description", EntityType.CONTACT, trigger, condition, actions,
        user,
        user, null, null, null, 0L, null, true);
    given(workflowService.update(eq(1L), argThat(workflowRequest ->
        {
          List<ValueType> mockList = List.of(ARRAY, PLAIN, OBJECT);
          List<ValueType> valueTypeList = new ArrayList<>();
          workflowRequest.getActions().stream().filter(action -> action.getType().equals(ActionType.EDIT_PROPERTY)).forEach(action -> {
            EditPropertyAction payload = (EditPropertyAction) action.getPayload();
            valueTypeList.add(payload.getValueType());
          });
          return workflowRequest.getName().equalsIgnoreCase("Workflow 1")
              && workflowRequest.getDescription().equalsIgnoreCase("Workflow Description")
              && workflowRequest.getEntityType().equals(EntityType.CONTACT)
              && workflowRequest.getTrigger().getName().equals(TriggerType.EVENT)
              && workflowRequest.getTrigger().getTriggerFrequency().equals(TriggerFrequency.CREATED)
              && workflowRequest.getCondition().getConditionType().equals(ConditionType.FOR_ALL)
              && valueTypeList.size() == 21
              && valueTypeList.containsAll(mockList)
              && workflowRequest.isActive();
        }
    ))).willReturn(Mono.just(workflowDetail));
    //when
    var workflowResponse = buildWebClient()
        .put()
        .uri("/v1/workflows/1")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requestPayload)
        .retrieve()
        .bodyToFlux(String.class).blockLast();
    //then
    var expectedResponse =
        getResourceAsString("classpath:contracts/workflow/api/update-workflow-response-with-entity-contact.json");
    JSONAssert.assertEquals(expectedResponse, workflowResponse, false);

  }

  @Test
  public void givenWorkflowRequest_withEntityDeal_shouldCreateIt() throws IOException {
    //given
    var requestPayload = getResourceAsString("classpath:contracts/workflow/api/create-workflow-request-with-entity-deal.json");
    given(workflowService.create(argThat(workflowRequest ->
        {
          List<ValueType> mockList = List.of(ARRAY, PLAIN, OBJECT);
          List<ValueType> valueTypeList = new ArrayList<>();
          workflowRequest.getActions().stream().filter(action -> action.getType().equals(ActionType.EDIT_PROPERTY)).forEach(action -> {
            EditPropertyAction payload = (EditPropertyAction) action.getPayload();
            valueTypeList.add(payload.getValueType());
          });
          return workflowRequest.getName().equalsIgnoreCase("Workflow 1")
              && workflowRequest.getDescription().equalsIgnoreCase("Workflow Description")
              && workflowRequest.getEntityType().equals(EntityType.DEAL)
              && workflowRequest.getTrigger().getName().equals(TriggerType.EVENT)
              && workflowRequest.getTrigger().getTriggerFrequency().equals(TriggerFrequency.CREATED)
              && workflowRequest.getCondition().getConditionType().equals(ConditionType.FOR_ALL)
              && workflowRequest.getActions().stream().allMatch(action -> action.getType().equals(ActionType.EDIT_PROPERTY))
              && valueTypeList.containsAll(mockList)
              && workflowRequest.getActions().size() == 9
              && workflowRequest.isActive();
        }
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
  public void givenWorkflowRequest_withEntityDeal_shouldUpdateIt() throws IOException, JSONException {
    //given
    var requestPayload = getResourceAsString("classpath:contracts/workflow/api/update-workflow-request-with-entity-deal.json");
    ObjectMapper objectMapper = new ObjectMapper();
    WorkflowTrigger trigger = new WorkflowTrigger(TriggerType.EVENT, TriggerFrequency.CREATED);
    Condition condition = new Condition(ConditionType.FOR_ALL.name(), null);
    List<ActionResponse> actions =
        List.of(
            new ActionResponse(ActionType.EDIT_PROPERTY,
                new EditPropertyAction("ownedBy", objectMapper.readValue("{\"id\":13,\"name\":\"James BondUpdated\"}", Object.class), OBJECT)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("name", "deal by workflow updated", PLAIN)),
            new ActionResponse(ActionType.EDIT_PROPERTY,
                new EditPropertyAction("estimatedValue", objectMapper.readValue("{\"currencyId\":1,\"value\":10000}", Object.class), OBJECT)),
            new ActionResponse(ActionType.EDIT_PROPERTY,
                new EditPropertyAction("actualValue", objectMapper.readValue("{\"currencyId\":2,\"value\":20000}", Object.class), OBJECT)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("estimatedClosureOn", "2021-01-15T06:30:00.000Z", PLAIN)),
            new ActionResponse(ActionType.EDIT_PROPERTY,
                new EditPropertyAction("product", objectMapper.readValue("{\"id\":2,\"name\":\"Marketing Service Updated\"}", Object.class), OBJECT)),
            new ActionResponse(ActionType.EDIT_PROPERTY,
                new EditPropertyAction("pipeline", objectMapper
                    .readValue("{\"id\":11,\"name\":\"Test Deal Pipeline Updated\",\"stage\":{\"id\":1,\"name\":\"Open\"}}", Object.class),
                    OBJECT)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("associatedContacts",
                objectMapper.readValue("[{\"id\":14,\"name\":\"Tony StarkUpdated\"}]", Object[].class), ARRAY)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("company",
                objectMapper.readValue("{\"id\":15,\"name\":\"Dell enterprises Updated\"}", Object.class), OBJECT)));

    User user = new User(5000L, "Tony Stark");
    WorkflowDetail workflowDetail = new WorkflowDetail(1L, "Workflow 1", "Workflow Description", EntityType.DEAL, trigger, condition, actions,
        user,
        user, null, null, null, 0L, null, true);
    given(workflowService.update(eq(1L), argThat(workflowRequest ->
        {
          List<ValueType> mockList = List.of(OBJECT, PLAIN, ARRAY);
          List<ValueType> valueTypeList = new ArrayList<>();
          workflowRequest.getActions().stream().filter(action -> action.getType().equals(ActionType.EDIT_PROPERTY)).forEach(action -> {
            EditPropertyAction payload = (EditPropertyAction) action.getPayload();
            valueTypeList.add(payload.getValueType());
          });
          return workflowRequest.getName().equalsIgnoreCase("Workflow 1")
              && workflowRequest.getDescription().equalsIgnoreCase("Workflow Description")
              && workflowRequest.getEntityType().equals(EntityType.DEAL)
              && workflowRequest.getTrigger().getName().equals(TriggerType.EVENT)
              && workflowRequest.getTrigger().getTriggerFrequency().equals(TriggerFrequency.CREATED)
              && workflowRequest.getCondition().getConditionType().equals(ConditionType.FOR_ALL)
              && workflowRequest.getActions().stream().allMatch(action -> action.getType().equals(ActionType.EDIT_PROPERTY))
              && valueTypeList.containsAll(mockList)
              && workflowRequest.getActions().size() == 9
              && workflowRequest.isActive();
        }
    ))).willReturn(Mono.just(workflowDetail));
    //when
    var workflowResponse = buildWebClient()
        .put()
        .uri("/v1/workflows/1")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requestPayload)
        .retrieve()
        .bodyToMono(String.class).block();
    //then
    var expectedResponse =
        getResourceAsString("classpath:contracts/workflow/api/update-workflow-response-with-entity-deal.json");
    JSONAssert.assertEquals(expectedResponse, workflowResponse, false);

  }

  @Test
  public void givenContactWorkflowRequest_withIdNameCondition_shouldCreateIt() throws JSONException, IOException {
    //given
    var requestPayload =
        getResourceAsString("classpath:contracts/workflow/api/contact-workflow-request-with-idName-condition.json");
    ObjectMapper objectMapper = new ObjectMapper();
    WorkflowTrigger trigger = new WorkflowTrigger(TriggerType.EVENT, TriggerFrequency.CREATED);
    Condition condition = new Condition(ConditionType.FOR_ALL.name(), null);
    List<ActionResponse> actions =
        List.of(
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("salutation", "1319", PLAIN)),
            new ActionResponse(ActionType.EDIT_PROPERTY,
                new EditPropertyAction("company", objectMapper.readValue("{\"id\":201,\"name\":\"Uflex\"}", Object.class), OBJECT)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("associatedDeals",
                objectMapper.readValue("[{\"id\":100,\"name\":\"BestDeal\"}]", Object[].class), ARRAY)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("phoneNumbers",
                objectMapper
                    .readValue("[{\"type\":\"HOME\",\"code\":\"0253\",\"value\":\"+0253\",\"dialCode\":\"2459817\",\"isPrimary\":true}]",
                        Object[].class),
                ARRAY)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("emails",
                objectMapper.readValue("[{\"type\":\"OFFICE\",\"value\":\"john150@outlook.com\",\"isPrimary\":true}]", Object[].class), ARRAY)));

    given(workflowService.create(argThat(workflowRequest ->
        {
          List<ValueType> mockList = List.of(OBJECT, PLAIN, ARRAY);
          List<ValueType> valueTypeList = new ArrayList<>();
          workflowRequest.getActions().stream().filter(action -> action.getType().equals(ActionType.EDIT_PROPERTY)).forEach(action -> {
            EditPropertyAction payload = (EditPropertyAction) action.getPayload();
            valueTypeList.add(payload.getValueType());
          });
          ExpressionElement expressionElement = workflowRequest.getCondition().getConditions().get(0);
          return workflowRequest.getName().equalsIgnoreCase("Workflow 1")
              && workflowRequest.getDescription().equalsIgnoreCase("Workflow Description")
              && workflowRequest.getCondition().getConditionType().equals(ConditionType.CONDITION_BASED)
              && expressionElement.getName().equals("createdBy")
              && expressionElement.getOperator().equals(Operator.EQUAL)
              && ((LinkedHashMap) expressionElement.getValue()).get("id").equals(200)
              && ((LinkedHashMap) expressionElement.getValue()).get("name").equals("John Cena");
        })
    )).willReturn(Mono.just(new WorkflowSummary(1L)));
    //when
    var workflowResponse = buildWebClient()
        .post()
        .uri("/v1/workflows")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requestPayload)
        .retrieve()
        .bodyToMono(String.class).block();
    //then
    var expectedResponse =
        getResourceAsString("classpath:contracts/workflow/api/create-workflow-response-with-entity-contact.json");
    JSONAssert.assertEquals(expectedResponse, workflowResponse, false);
  }


  private String getResourceAsString(String resourcePath) throws IOException {
    var resource = resourceLoader.getResource(resourcePath);
    File file = resource.getFile();
    return FileUtils.readFileToString(file, "UTF-8");
  }


}
