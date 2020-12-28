package com.kylas.sales.workflow.api;

import static com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.ValueType.ARRAY;
import static com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.ValueType.PLAIN;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.kylas.sales.workflow.api.request.FilterRequest;
import com.kylas.sales.workflow.api.request.FilterRequest.Filter;
import com.kylas.sales.workflow.api.request.WorkflowRequest;
import com.kylas.sales.workflow.api.response.WorkflowDetail;
import com.kylas.sales.workflow.api.response.WorkflowSummary;
import com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction;
import com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.ValueType;
import com.kylas.sales.workflow.common.dto.ActionDetail.ReassignAction;
import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.common.dto.User;
import com.kylas.sales.workflow.common.dto.WorkflowCondition;
import com.kylas.sales.workflow.common.dto.WorkflowTrigger;
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
import java.util.List;
import java.util.Set;
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
  public void givenWorkflow_withDifferentValueTypes_shouldCreateIt() {
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
  public void givenWorkflowWithMultipleActions_andDifferentValueTypes_shouldCreateIt() {
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
  public void givenWorkflowUpdate_withDifferentValueTypes_shouldUpdateIt() {
    //given
    var requestPayload = getResourceAsString("classpath:contracts/workflow/api/update-workflow-request.json");
    ObjectMapper objectMapper = new ObjectMapper();
    WorkflowTrigger trigger = new WorkflowTrigger(TriggerType.EVENT, TriggerFrequency.CREATED);
    WorkflowCondition condition = new WorkflowCondition(ConditionType.FOR_ALL, null);
    Set<ActionResponse> actions =
        Set.of(
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("salutation", 1319, PLAIN)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("dnd", true, PLAIN)),
            new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("city", "PUNE", PLAIN)),
            new ActionResponse(ActionType.REASSIGN, new ReassignAction(20003L)));
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
  public void givenDeactivatedWorkflow_shouldCreateIt() {
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
  public void givenWorkflowId_shouldGetIt() {
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
    given(workflowService.get(workflowId)).willReturn(workflowDetail);
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
  public void givenWorkflowId_shouldActivateIt() {
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
    given(workflowService.activate(workflowId)).willReturn(workflowDetail);
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
  public void givenWorkflowId_shouldDeactivateIt() {
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
    given(workflowService.deactivate(workflowId)).willReturn(workflowDetail);
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
  public void givenListRequest_shouldGetPageableListingPage() {
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
  public void givenSearchRequest_tryToSortOnLastTriggeredAt_shouldGetPageableListingPage() {
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

    Sort sortByLastTriggeredAt = Sort.by(Order.desc("lastTriggeredAt"));
    Sort expectedSortable = Sort.by(Order.desc("workflowExecutedEvent.lastTriggeredAt"));

    given(
            workflowService.search(
                argThat(new PageableMatcher(0, 10, expectedSortable)),
                argThat(filterRequest -> filterRequest.isEmpty())))
        .willReturn(
            Mono.just(
                new PageImpl<>(
                    asList(workflowDetail1, workflowDetail2),
                    PageRequest.of(0, 10, sortByLastTriggeredAt),
                    12)));
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
  public void givenSearchRequest_tryToSortOnLastUpdatedAt_shouldGetPageableListingPage() {
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

    Sort sortByLastTriggeredAt = Sort.by(Order.desc("lastUpdatedAt"));

    given(
            workflowService.search(
                argThat(new PageableMatcher(0, 10, sortByLastTriggeredAt)),
                argThat(filterRequest -> filterRequest.get().getFilters().isEmpty())))
        .willReturn(
            Mono.just(
                new PageImpl<>(
                    asList(workflowDetail1, workflowDetail2),
                    PageRequest.of(0, 10, sortByLastTriggeredAt),
                    12)));
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
  public void givenFilterSearchRequest_shouldSearch() {
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

    Sort sortByLastTriggeredAt = Sort.by(Order.desc("lastUpdatedAt"));
    List<Filter> filters = new ArrayList<>();
    filters.add(new Filter("equals", "entityType", "string", "LEAD"));
    FilterRequest expectedFilter = new FilterRequest(filters);
    given(
            workflowService.search(
                argThat(new PageableMatcher(0, 10, sortByLastTriggeredAt)),
                argThat(new FilterRequestMatcher(expectedFilter))))
        .willReturn(
            Mono.just(
                new PageImpl<>(
                    asList(workflowDetail1, workflowDetail2),
                    PageRequest.of(0, 10, sortByLastTriggeredAt),
                    12)));
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
  public void givenInvalidFilter_shouldThrow() {
    // given
    User createdBy = new User(101L, "Tony Start");
    var updatedBy = new User(102L, "Steve Roger");


    Sort sortByLastTriggeredAt = Sort.by(Order.desc("lastUpdatedAt"));
    List<Filter> filters = new ArrayList<>();
    filters.add(new Filter("equals", "entityType", "string", "LEAD"));
    FilterRequest expectedFilter = new FilterRequest(filters);
    given(
        workflowService.search(any(),any())).willThrow(new InvalidFilterException());
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
  public void givenWorkflow_withReassignAction_shouldCreateIt() {
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
  public void givenWorkflow_withReassignAction_shouldUpdateIt() {
    //given
    var requestPayload = getResourceAsString("classpath:contracts/reassign/update-workflow-with-reassign-action-request.json");
    WorkflowTrigger trigger = new WorkflowTrigger(TriggerType.EVENT, TriggerFrequency.CREATED);
    WorkflowCondition condition = new WorkflowCondition(ConditionType.FOR_ALL, null);
    Set<ActionResponse> actions =
        Set.of(
            new ActionResponse(ActionType.REASSIGN, new ReassignAction(20003L)));
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
