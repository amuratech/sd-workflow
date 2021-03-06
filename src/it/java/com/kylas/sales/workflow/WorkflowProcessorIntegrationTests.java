package com.kylas.sales.workflow;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okForContentType;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.kylas.sales.workflow.mq.event.DealEvent.getDealCreatedEventName;
import static com.kylas.sales.workflow.mq.event.DealEvent.getDealUpdatedEventName;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.context.support.TestPropertySourceUtils.addInlinedPropertiesToEnvironment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylas.sales.workflow.WorkflowProcessorIntegrationTests.TestMqSetup;
import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import com.kylas.sales.workflow.domain.WorkflowFacade;
import com.kylas.sales.workflow.domain.user.User;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.mq.event.DealEvent;
import com.kylas.sales.workflow.mq.event.LeadEvent;
import com.kylas.sales.workflow.security.AuthService;
import com.kylas.sales.workflow.stubs.UserStub;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.json.JSONException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.RabbitMQContainer;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@AutoConfigureWireMock(port = 9090)
@ContextConfiguration(initializers = {TestMqSetup.class, TestDatabaseInitializer.class})
public class WorkflowProcessorIntegrationTests {

  static final String SALES_EXCHANGE = "ex.sales";
  static final String DEAL_EXCHANGE = "ex.deal";
  static final String WORKFLOW_EXCHANGE = "ex.workflow";
  static final String SALES_LEAD_UPDATE_QUEUE = "q.workflow.lead.update.sales";
  static final String SALES_LEAD_UPDATE_QUEUE_NEW = "q.workflow.lead.update.sales_new";
  static final String LEAD_UPDATE_COMMAND_QUEUE = "workflow.lead.update";
  static final String LEAD_UPDATE_COMMAND_QUEUE_1 = "workflow.lead.update_1";
  static final String SALES_LEAD_REASSIGN_QUEUE = "workflow.lead.reassign.sales";
  static final String SALES_LEAD_REASSIGN_QUEUE_NEW = "workflow.lead.reassign.sales_new";
  static final String LEAD_REASSIGN_COMMAND_QUEUE = "workflow.lead.reassign";
  static final String DEAL_UPDATE_QUEUE = "q.workflow.deal.update";
  static final String DEAL_UPDATE_QUEUE_NEW = "q.workflow.deal.update_new";
  static final String DEAL_UPDATE_COMMAND = "workflow.deal.update";
  static final String DEAL_REASSIGN_QUEUE = "workflow.deal.reassign.deal_service";
  static final String DEAL_REASSIGN_QUEUE_NEW = "workflow.deal.reassign.deal_service_new";
  static final String DEAL_REASSIGN_COMMAND_QUEUE = "workflow.deal.reassign";
  static final String SALES_LEAD_UPDATE_QUEUE_NEW_2 = "q.workflow.lead.update.sales_new2";

  private static RabbitMQContainer rabbitMQContainer =
      new RabbitMQContainer("rabbitmq:3.7-management-alpine");
  private MockMqListener mockMqListener = new MockMqListener();

  @Autowired private ConnectionFactory connectionFactory;
  @Autowired private AmqpAdmin rabbitAdmin;
  @Autowired private RabbitTemplate rabbitTemplate;
  @Autowired private ObjectMapper objectMapper;
  @Autowired WorkflowFacade workflowFacade;
  @MockBean AuthService authService;

  @BeforeAll
  public static void initialise() {
    rabbitMQContainer.start();
  }

  @AfterAll
  public static void tearDown() {
    rabbitMQContainer.stop();
  }

  @Test
  @Sql("/test-scripts/insert-create-lead-workflow.sql")
  public void givenLeadCreateEvent_shouldUpdatePropertyAndPublishCommand()
      throws IOException, InterruptedException, JSONException {
    // given
    String authenticationToken = "some-token";
    User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);
    given(authService.getAuthenticationToken()).willReturn(authenticationToken);

    String resourceAsString = getResourceAsString("/contracts/mq/events/lead-created-event.json");
    LeadEvent leadEvent = objectMapper.readValue(resourceAsString, LeadEvent.class);
    initializeRabbitMqListener(LEAD_UPDATE_COMMAND_QUEUE, SALES_LEAD_UPDATE_QUEUE);
    // when
    rabbitTemplate.convertAndSend(SALES_EXCHANGE, LeadEvent.getLeadCreatedEventName(), leadEvent);
    // then
    mockMqListener.latch.await(3, TimeUnit.SECONDS);
    JSONAssert.assertEquals(
        getResourceAsString("/contracts/mq/command/lead-update-patch-command.json"),
        mockMqListener.actualMessage,
        JSONCompareMode.STRICT);

    Workflow workflow = workflowFacade.get(301);
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getLastTriggeredAt()).isNotNull();
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);
  }

  @Test
  @Sql("/test-scripts/insert-workflow.sql")
  public void givenLeadCreateEvent_shouldExecuteTwoWorkflowAndUpdateExecutedEvents()
      throws IOException, InterruptedException {
    // given
    User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    String resourceAsString = getResourceAsString("/contracts/mq/events/lead-created-event.json");
    LeadEvent leadEvent = objectMapper.readValue(resourceAsString, LeadEvent.class);
    // when
    rabbitTemplate.convertAndSend(SALES_EXCHANGE, LeadEvent.getLeadCreatedEventName(), leadEvent);
    // then
    mockMqListener.latch.await(3, TimeUnit.SECONDS);

    Workflow workflow301 = workflowFacade.get(301);
    Assertions.assertThat(workflow301.getWorkflowExecutedEvent().getLastTriggeredAt()).isNotNull();
    Assertions.assertThat(workflow301.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);

    Workflow workflow302 = workflowFacade.get(302);
    Assertions.assertThat(workflow302.getWorkflowExecutedEvent().getLastTriggeredAt()).isNotNull();
    Assertions.assertThat(workflow302.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(16);
  }

  @Test
  @Sql("/test-scripts/insert-update-lead-workflow.sql")
  public void givenLeadUpdatedEvent_shouldUpdatePropertyAndPublishCommand()
      throws IOException, InterruptedException, JSONException {
    // given
    User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    String resourceAsString =
        getResourceAsString("/contracts/mq/events/sales-lead-updated-event-payload.json");
    LeadEvent leadEvent = objectMapper.readValue(resourceAsString, LeadEvent.class);
    initializeRabbitMqListener(LEAD_UPDATE_COMMAND_QUEUE, SALES_LEAD_UPDATE_QUEUE_NEW);
    // when
    rabbitTemplate.convertAndSend(SALES_EXCHANGE, LeadEvent.getLeadUpdatedEventName(), leadEvent);
    // then
    mockMqListener.latch.await(3, TimeUnit.SECONDS);
    JSONAssert.assertEquals(
        getResourceAsString("/contracts/mq/command/lead-update-patch-command-3.json"),
        mockMqListener.actualMessage,
        JSONCompareMode.STRICT);

    Workflow workflow = workflowFacade.get(301);
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getLastTriggeredAt()).isNotNull();
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);
  }

  @Test
  @Sql("/test-scripts/insert-update-lead-workflow.sql")
  public void givenLeadUpdatedEvent_tryToReProcessSameWorkflow_shouldNotProcess()
      throws IOException, InterruptedException {
    // given
    User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    String resourceAsString =
        getResourceAsString(
            "/contracts/mq/events/sales-lead-updated-event-payload-with-executedWorkflow.json");
    LeadEvent leadEvent = objectMapper.readValue(resourceAsString, LeadEvent.class);
    initializeRabbitMqListener(LEAD_UPDATE_COMMAND_QUEUE, SALES_LEAD_UPDATE_QUEUE);
    // when
    rabbitTemplate.convertAndSend(SALES_EXCHANGE, LeadEvent.getLeadUpdatedEventName(), leadEvent);
    // then
    mockMqListener.latch.await(3, TimeUnit.SECONDS);

    Workflow workflow = workflowFacade.get(301);
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getLastTriggeredAt()).isNull();
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(151);
  }

  @Nested
  @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
  @AutoConfigureTestDatabase(replace = Replace.NONE)
  @ContextConfiguration(initializers = {TestMqSetup.class, TestDatabaseInitializer.class})
  @DisplayName("Tests that verify workflow condition evaluation")
  class WorkflowConditionIntegrationTests {

    @Test
    @Sql("/test-scripts/integration/insert-workflows-with-condition.sql")
    public void givenLeadUpdatedEvent_shouldTriggerWorkflowsConditionally()
        throws IOException, InterruptedException {
      // given
      User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("user 1");
      given(authService.getLoggedInUser()).willReturn(aUser);

      String resourceAsString =
          getResourceAsString(
              "/contracts/mq/events/lead-created-triggering-conditional-workflows.json");
      LeadEvent leadEvent = objectMapper.readValue(resourceAsString, LeadEvent.class);
      initializeRabbitMqListener(LEAD_UPDATE_COMMAND_QUEUE, SALES_LEAD_UPDATE_QUEUE);
      // when
      rabbitTemplate.convertAndSend(SALES_EXCHANGE, LeadEvent.getLeadUpdatedEventName(), leadEvent);
      // then
      mockMqListener.latch.await(3, TimeUnit.SECONDS);

      Workflow workflow301 = workflowFacade.get(301);
      Assertions.assertThat(workflow301.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(0);

      Workflow workflow302 = workflowFacade.get(302);
      Assertions.assertThat(workflow302.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(1);
    }


    @Test
    @Sql("/test-scripts/integration/insert-lead-workflow-with-condition-and-old-value-trigger.sql")
    public void givenLeadUpdatedEvent_withOldValueTriggerOn_shouldTriggerWorkflowsConditionally()
        throws IOException, InterruptedException {
      // given
      User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("user 1");
      given(authService.getLoggedInUser()).willReturn(aUser);

      String resourceAsString =
          getResourceAsString(
              "/contracts/mq/events/lead-created-triggering-conditional-workflows.json");
      LeadEvent leadEvent = objectMapper.readValue(resourceAsString, LeadEvent.class);
      initializeRabbitMqListener(LEAD_UPDATE_COMMAND_QUEUE, SALES_LEAD_UPDATE_QUEUE);
      // when
      rabbitTemplate.convertAndSend(SALES_EXCHANGE, LeadEvent.getLeadUpdatedEventName(), leadEvent);
      // then
      mockMqListener.latch.await(3, TimeUnit.SECONDS);

      Workflow workflow301 = workflowFacade.get(301);
      Assertions.assertThat(workflow301.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(1);

    }


    @Test
    @Sql("/test-scripts/integration/insert-lead-workflow-with-condition-and-is-changed-trigger.sql")
    public void givenLeadUpdatedEvent_withIsChangedTriggerOn_shouldTriggerWorkflowsConditionally()
        throws IOException, InterruptedException {
      // given
      User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("user 1");
      given(authService.getLoggedInUser()).willReturn(aUser);

      String resourceAsString =
          getResourceAsString(
              "/contracts/mq/events/lead-created-triggering-conditional-workflows.json");
      LeadEvent leadEvent = objectMapper.readValue(resourceAsString, LeadEvent.class);
      initializeRabbitMqListener(LEAD_UPDATE_COMMAND_QUEUE, SALES_LEAD_UPDATE_QUEUE);
      // when
      rabbitTemplate.convertAndSend(SALES_EXCHANGE, LeadEvent.getLeadUpdatedEventName(), leadEvent);
      // then
      mockMqListener.latch.await(3, TimeUnit.SECONDS);

      Workflow workflow301 = workflowFacade.get(301);
      Assertions.assertThat(workflow301.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(1);
    }

    @Test
    @Sql("/test-scripts/integration/insert-lead-workflow-with-condition-and-is-changed-trigger-idname-field.sql")
    public void givenLeadUpdatedEvent_withIsChangedTriggerOnIdNameField_shouldTriggerWorkflowsConditionally()
        throws IOException, InterruptedException {
      // given
      User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("user 1");
      given(authService.getLoggedInUser()).willReturn(aUser);

      String resourceAsString =
          getResourceAsString(
              "/contracts/mq/events/lead-created-triggering-conditional-workflows-id-name-field.json");
      LeadEvent leadEvent = objectMapper.readValue(resourceAsString, LeadEvent.class);
      initializeRabbitMqListener(LEAD_UPDATE_COMMAND_QUEUE_1, SALES_LEAD_UPDATE_QUEUE);
      // when
      rabbitTemplate.convertAndSend(SALES_EXCHANGE, LeadEvent.getLeadUpdatedEventName(), leadEvent);
      // then
      mockMqListener.latch.await(3, TimeUnit.SECONDS);

      Workflow workflow301 = workflowFacade.get(301);
      Assertions.assertThat(workflow301.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(1);
    }


    @Test
    @Sql("/test-scripts/integration/insert-workflow-with-multiple-condition.sql")
    public void givenLeadUpdatedEvent_withMultipleConditions_shouldTriggerWorkflowsConditionally()
        throws IOException, InterruptedException {
      // given
      User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("user 1");
      given(authService.getLoggedInUser()).willReturn(aUser);

      String resourceAsString =
          getResourceAsString(
              "/contracts/mq/events/lead-created-triggering-conditional-workflows.json");
      LeadEvent leadEvent = objectMapper.readValue(resourceAsString, LeadEvent.class);
      initializeRabbitMqListener(LEAD_UPDATE_COMMAND_QUEUE, SALES_LEAD_UPDATE_QUEUE);
      // when
      rabbitTemplate.convertAndSend(SALES_EXCHANGE, LeadEvent.getLeadUpdatedEventName(), leadEvent);
      // then
      mockMqListener.latch.await(3, TimeUnit.SECONDS);

      Workflow workflow301 = workflowFacade.get(301);
      Assertions.assertThat(workflow301.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(0);

      Workflow workflow302 = workflowFacade.get(302);
      Assertions.assertThat(workflow302.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(1);
    }

    @Test
    @Sql("/test-scripts/integration/insert-lead-workflow-with-old-value-trigger-and-multiple-conditions.sql")
    public void givenLeadUpdatedEvent_withOldValueTriggerOnAndMultipleConditions_shouldTriggerWorkflowsConditionally()
        throws IOException, InterruptedException {
      // given
      User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("user 1");
      given(authService.getLoggedInUser()).willReturn(aUser);

      String resourceAsString =
          getResourceAsString(
              "/contracts/mq/events/lead-created-triggering-conditional-workflows.json");
      LeadEvent leadEvent = objectMapper.readValue(resourceAsString, LeadEvent.class);
      initializeRabbitMqListener(LEAD_UPDATE_COMMAND_QUEUE, SALES_LEAD_UPDATE_QUEUE);
      // when
      rabbitTemplate.convertAndSend(SALES_EXCHANGE, LeadEvent.getLeadUpdatedEventName(), leadEvent);
      // then
      mockMqListener.latch.await(3, TimeUnit.SECONDS);

      Workflow workflow301 = workflowFacade.get(301);
      Assertions.assertThat(workflow301.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(1);

      Workflow workflow302 = workflowFacade.get(302);
      Assertions.assertThat(workflow302.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(0);
    }
  }

  @Nested
  @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
  @AutoConfigureTestDatabase(replace = Replace.NONE)
  @ContextConfiguration(initializers = {TestMqSetup.class, TestDatabaseInitializer.class})
  @DisplayName("Tests that execute webhooks on event")
  class WebhookIntegrationTests {

    @Test
    @Sql("/test-scripts/create-webhook-workflow.sql")
    public void givenLeadCreateEvent_usingMethodGET_shouldExecute()
        throws IOException, InterruptedException {
      // given
      String authenticationToken = "some-token";
      User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("user 1");
      given(authService.getLoggedInUser()).willReturn(aUser);
      given(authService.getAuthenticationToken()).willReturn(authenticationToken);

      stubFor(
          get("/iam/v1/users/10")
              .withHeader(AUTHORIZATION, matching("Bearer " + authenticationToken))
              .willReturn(
                  okForContentType(
                      MediaType.APPLICATION_JSON_VALUE,
                      getResourceAsString("/contracts/user/responses/user-details-by-id.json"))));

      stubFor(
          get("/iam/v1/tenants")
              .withHeader(AUTHORIZATION, matching("Bearer " + authenticationToken))
              .willReturn(
                  okForContentType(
                      MediaType.APPLICATION_JSON_VALUE,
                      getResourceAsString("/contracts/user/responses/tenant-details.json"))));

      String resourceAsString =
          getResourceAsString("/contracts/mq/events/lead-created-v2-event.json");
      LeadEvent leadEvent = objectMapper.readValue(resourceAsString, LeadEvent.class);
      initializeRabbitMqListener(LEAD_UPDATE_COMMAND_QUEUE, SALES_LEAD_UPDATE_QUEUE);
      // when
      rabbitTemplate.convertAndSend(SALES_EXCHANGE, LeadEvent.getLeadCreatedEventName(), leadEvent);
      mockMqListener.latch.await(3, TimeUnit.SECONDS);
      // then
      Workflow workflow = workflowFacade.get(301);
      Assertions.assertThat(workflow.getWorkflowExecutedEvent().getLastTriggeredAt()).isNotNull();
      Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);
    }

    @Test
    @Sql("/test-scripts/create-post-webhook-workflow.sql")
    public void givenLeadCreateEvent_usingMethodPOST_shouldExecute()
        throws IOException, InterruptedException {
      // given
      String authenticationToken = "some-token";
      User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("user 1");
      given(authService.getLoggedInUser()).willReturn(aUser);
      given(authService.getAuthenticationToken()).willReturn(authenticationToken);

      stubFor(
          get("/iam/v1/users/10")
              .withHeader(AUTHORIZATION, matching("Bearer " + authenticationToken))
              .willReturn(
                  okForContentType(
                      MediaType.APPLICATION_JSON_VALUE,
                      getResourceAsString("/contracts/user/responses/user-details-by-id.json"))));

      stubFor(
          get("/iam/v1/tenants")
              .withHeader(AUTHORIZATION, matching("Bearer " + authenticationToken))
              .willReturn(
                  okForContentType(
                      MediaType.APPLICATION_JSON_VALUE,
                      getResourceAsString("/contracts/user/responses/tenant-details.json"))));

      String resourceAsString =
          getResourceAsString("/contracts/mq/events/lead-created-v2-event.json");
      LeadEvent leadEvent = objectMapper.readValue(resourceAsString, LeadEvent.class);
      initializeRabbitMqListener(LEAD_UPDATE_COMMAND_QUEUE, SALES_LEAD_UPDATE_QUEUE);
      // when
      rabbitTemplate.convertAndSend(SALES_EXCHANGE, LeadEvent.getLeadCreatedEventName(), leadEvent);
      mockMqListener.latch.await(3, TimeUnit.SECONDS);
      // then
      Workflow workflow = workflowFacade.get(301);
      Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);
    }

    @Test
    @Sql("/test-scripts/create-put-webhook-workflow.sql")
    public void givenLeadCreateEvent_usingMethodPUT_shouldExecute()
        throws IOException, InterruptedException {
      // given
      String authenticationToken = "some-token";
      User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("user 1");
      given(authService.getLoggedInUser()).willReturn(aUser);
      given(authService.getAuthenticationToken()).willReturn(authenticationToken);

      stubFor(
          get("/iam/v1/users/10")
              .willReturn(
                  okForContentType(
                      MediaType.APPLICATION_JSON_VALUE,
                      getResourceAsString("/contracts/user/responses/user-details-by-id.json"))));

      stubFor(
          get("/iam/v1/tenants")
              .willReturn(
                  okForContentType(
                      MediaType.APPLICATION_JSON_VALUE,
                      getResourceAsString("/contracts/user/responses/tenant-details.json"))));

      String resourceAsString =
          getResourceAsString("/contracts/mq/events/lead-created-v2-event.json");
      LeadEvent leadEvent = objectMapper.readValue(resourceAsString, LeadEvent.class);
      initializeRabbitMqListener(LEAD_UPDATE_COMMAND_QUEUE, SALES_LEAD_UPDATE_QUEUE);
      // when
      rabbitTemplate.convertAndSend(SALES_EXCHANGE, LeadEvent.getLeadCreatedEventName(), leadEvent);
      mockMqListener.latch.await(3, TimeUnit.SECONDS);
      // then
      Workflow workflow = workflowFacade.get(301);
      Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);
    }
  }

  @Nested
  @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
  @AutoConfigureTestDatabase(replace = Replace.NONE)
  @ContextConfiguration(initializers = {TestMqSetup.class, TestDatabaseInitializer.class})
  @DisplayName("Tests that publish reassign event when entity created/updated")
  class ReassignIntegrationTests {

    @Test
    @Sql("/test-scripts/insert-reassign-lead-workflow.sql")
    public void givenLeadCreatedEvent_shouldPublish_reassignEvent()
        throws IOException, InterruptedException, JSONException {
      // given
      String authenticationToken = "some-token";
      User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("user 1");
      given(authService.getLoggedInUser()).willReturn(aUser);
      given(authService.getAuthenticationToken()).willReturn(authenticationToken);

      String resourceAsString =
          getResourceAsString("/contracts/mq/events/lead-created-reassign-event.json");
      LeadEvent leadEvent = objectMapper.readValue(resourceAsString, LeadEvent.class);
      initializeRabbitMqListener(LEAD_REASSIGN_COMMAND_QUEUE, SALES_LEAD_REASSIGN_QUEUE);
      // when
      rabbitTemplate.convertAndSend(SALES_EXCHANGE, LeadEvent.getLeadCreatedEventName(), leadEvent);
      // then
      mockMqListener.latch.await(3, TimeUnit.SECONDS);
      JSONAssert.assertEquals(
          getResourceAsString("/contracts/mq/command/lead-reassign-patch-command.json"),
          mockMqListener.actualMessage,
          JSONCompareMode.STRICT);

      Workflow workflow = workflowFacade.get(301);
      Assertions.assertThat(workflow.getWorkflowExecutedEvent().getLastTriggeredAt()).isNotNull();
      Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);
    }

    @Test
    @Sql("/test-scripts/insert-reassign-update-lead-workflow.sql")
    public void givenLeadUpdatedEvent_shouldPublish_reassignEvent()
        throws IOException, InterruptedException, JSONException {
      // given
      User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("user 1");
      given(authService.getLoggedInUser()).willReturn(aUser);

      String resourceAsString =
          getResourceAsString("/contracts/mq/events/sales-lead-reassigned-event-payload.json");
      LeadEvent leadEvent = objectMapper.readValue(resourceAsString, LeadEvent.class);
      initializeRabbitMqListener(LEAD_REASSIGN_COMMAND_QUEUE, SALES_LEAD_REASSIGN_QUEUE_NEW);
      // when
      rabbitTemplate.convertAndSend(SALES_EXCHANGE, LeadEvent.getLeadUpdatedEventName(), leadEvent);
      // then
      mockMqListener.latch.await(3, TimeUnit.SECONDS);
      JSONAssert.assertEquals(
          getResourceAsString("/contracts/mq/command/lead-reassign-patch-command-2.json"),
          mockMqListener.actualMessage,
          JSONCompareMode.STRICT);

      Workflow workflow = workflowFacade.get(301);
      Assertions.assertThat(workflow.getWorkflowExecutedEvent().getLastTriggeredAt()).isNotNull();
      Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);
    }

    @Test
    @Sql("/test-scripts/insert-reassign-deal-workflow.sql")
    public void givenDealCreatedEvent_shouldPublish_reassignEvent()
        throws IOException, InterruptedException, JSONException {
      // given
      String authenticationToken = "some-token";
      User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("user 1");
      given(authService.getLoggedInUser()).willReturn(aUser);
      given(authService.getAuthenticationToken()).willReturn(authenticationToken);

      String resourceAsString =
          getResourceAsString("/contracts/mq/events/deal-created-reassign-event.json");
      DealEvent dealCreatedEvent = objectMapper.readValue(resourceAsString, DealEvent.class);
      initializeRabbitMqListener(DEAL_REASSIGN_COMMAND_QUEUE, DEAL_REASSIGN_QUEUE);
      // when
      rabbitTemplate.convertAndSend(
          DEAL_EXCHANGE, DealEvent.getDealCreatedEventName(), dealCreatedEvent);
      // then
      mockMqListener.latch.await(3, TimeUnit.SECONDS);
      JSONAssert.assertEquals(
          getResourceAsString("/contracts/mq/command/deal-reassign-patch-command.json"),
          mockMqListener.actualMessage,
          JSONCompareMode.STRICT);

      Workflow workflow = workflowFacade.get(301);
      Assertions.assertThat(workflow.getWorkflowExecutedEvent().getLastTriggeredAt()).isNotNull();
      Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);
    }

    @Test
    @Sql("/test-scripts/insert-reassign-update-deal-workflow.sql")
    public void givenDealUpdatedEvent_shouldPublish_reassignEvent()
        throws IOException, InterruptedException, JSONException {
      // given
      User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("user 1");
      given(authService.getLoggedInUser()).willReturn(aUser);

      String resourceAsString =
          getResourceAsString("/contracts/mq/events/deal-updated-reassign-event-payload.json");
      DealEvent dealUpdatedEvent = objectMapper.readValue(resourceAsString, DealEvent.class);
      initializeRabbitMqListener(DEAL_REASSIGN_COMMAND_QUEUE, DEAL_REASSIGN_QUEUE_NEW);
      // when
      rabbitTemplate.convertAndSend(
          DEAL_EXCHANGE, DealEvent.getDealUpdatedEventName(), dealUpdatedEvent);
      // then
      mockMqListener.latch.await(3, TimeUnit.SECONDS);
      JSONAssert.assertEquals(
          getResourceAsString("/contracts/mq/command/deal-reassign-patch-command-2.json"),
          mockMqListener.actualMessage,
          JSONCompareMode.STRICT);

      Workflow workflow = workflowFacade.get(301);
      Assertions.assertThat(workflow.getWorkflowExecutedEvent().getLastTriggeredAt()).isNotNull();
      Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);
    }
  }

  @Nested
  @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
  @AutoConfigureTestDatabase(replace = Replace.NONE)
  @ContextConfiguration(initializers = {TestMqSetup.class, TestDatabaseInitializer.class})
  @DisplayName("Tests that publish event when deal created/updated")
  class DealWorkflowProcessorIntegrationTests {

    @Test
    @Sql("/test-scripts/integration/insert-deal-workflow-for-integration-test.sql")
    public void givenDealCreatedEvent_shouldUpdatePropertiesAndPublishCommand()
        throws IOException, InterruptedException, JSONException {
      // given
      String authenticationToken = "some-token";
      User aUser = UserStub.aUser(12L, 55L, true, true, true, true, true).withName("user 1");
      given(authService.getLoggedInUser()).willReturn(aUser);
      given(authService.getAuthenticationToken()).willReturn(authenticationToken);

      String resourceAsString = getResourceAsString("/contracts/mq/events/deal-created-event.json");
      DealEvent dealEvent = objectMapper.readValue(resourceAsString, DealEvent.class);
      initializeRabbitMqListener(DEAL_UPDATE_COMMAND, DEAL_UPDATE_QUEUE);
      // when
      rabbitTemplate.convertAndSend(DEAL_EXCHANGE, getDealCreatedEventName(), dealEvent);
      // then
      mockMqListener.latch.await(3, TimeUnit.SECONDS);
      JSONAssert.assertEquals(
          getResourceAsString("/contracts/mq/command/deal-create-patch-command.json"),
          mockMqListener.actualMessage,
          JSONCompareMode.STRICT);

      Workflow workflow = workflowFacade.get(301);
      Assertions.assertThat(workflow.getWorkflowExecutedEvent().getLastTriggeredAt()).isNotNull();
      Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);
    }

    @Test
    @Sql("/test-scripts/insert-deal-update-workflow.sql")
    public void givenDealUpdatedEvent_shouldUpdatePropertiesAndPublishCommand()
        throws IOException, InterruptedException, JSONException {
      // given
      String authenticationToken = "some-token";
      User aUser = UserStub.aUser(12L, 55L, true, true, true, true, true).withName("user 1");
      given(authService.getLoggedInUser()).willReturn(aUser);
      given(authService.getAuthenticationToken()).willReturn(authenticationToken);

      String resourceAsString = getResourceAsString("/contracts/mq/events/deal-updated-event.json");
      DealEvent dealEvent = objectMapper.readValue(resourceAsString, DealEvent.class);
      initializeRabbitMqListener(DEAL_UPDATE_COMMAND, DEAL_UPDATE_QUEUE_NEW);
      // when
      rabbitTemplate.convertAndSend(DEAL_EXCHANGE, getDealUpdatedEventName(), dealEvent);
      // then
      mockMqListener.latch.await(3, TimeUnit.SECONDS);
      JSONAssert.assertEquals(
          getResourceAsString("/contracts/mq/command/deal-update-patch-command.json"),
          mockMqListener.actualMessage,
          JSONCompareMode.STRICT);

      Workflow workflow = workflowFacade.get(301);
      Assertions.assertThat(workflow.getWorkflowExecutedEvent().getLastTriggeredAt()).isNotNull();
      Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);
    }
  }

  @Nested
  @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
  @AutoConfigureTestDatabase(replace = Replace.NONE)
  @ContextConfiguration(initializers = {TestMqSetup.class, TestDatabaseInitializer.class})
  public class WorkflowProcessorMultiActionIntegrationTests {

    @Test
    @Sql("/test-scripts/integration/multiple-action-lead-workflow.sql")
    public void givenLeadCreateEvent_shouldExecuteMultipleActions()
        throws IOException, InterruptedException, JSONException {
      // given
      User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("user 1");
      given(authService.getLoggedInUser()).willReturn(aUser);

      String resourceAsString =
          getResourceAsString("/contracts/mq/events/lead-created-v2-event.json");
      LeadEvent leadEvent = objectMapper.readValue(resourceAsString, LeadEvent.class);
      // when
      initializeRabbitMqListener(LEAD_UPDATE_COMMAND_QUEUE, SALES_LEAD_UPDATE_QUEUE_NEW_2);
      rabbitTemplate.convertAndSend(SALES_EXCHANGE, LeadEvent.getLeadCreatedEventName(), leadEvent);
      // then
      mockMqListener.latch.await(3, TimeUnit.SECONDS);
      JSONAssert.assertEquals(
          getResourceAsString("/contracts/mq/command/lead-update-patch-command-2.json"),
          mockMqListener.actualMessage,
          JSONCompareMode.STRICT);

      Workflow workflow = workflowFacade.get(301);
      Assertions.assertThat(workflow.getWorkflowExecutedEvent().getLastTriggeredAt()).isNotNull();
      Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);
    }
  }

  static class MockMqListener {

    CountDownLatch latch = new CountDownLatch(1);
    String actualMessage;

    public void receiveMessage(byte[] messageInBytes) {
      this.actualMessage = new String(messageInBytes);
    }
  }

  private String getResourceAsString(String resourcePath) throws IOException {
    var resource = new ClassPathResource(resourcePath);
    var file = resource.getFile();
    return FileUtils.readFileToString(file, "UTF-8");
  }

  private void initializeRabbitMqListener(String command, String consumerQueue) {

    rabbitAdmin.declareBinding(
        BindingBuilder.bind(new Queue(consumerQueue))
            .to(new TopicExchange(WORKFLOW_EXCHANGE))
            .with(command));

    MessageListenerAdapter listenerAdapter =
        new MessageListenerAdapter(mockMqListener, "receiveMessage");

    SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.setQueueNames(consumerQueue);
    container.setMessageListener(listenerAdapter);
    container.start();
  }

  @TestConfiguration
  public static class TestMqSetup
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      var withServices =
          rabbitMQContainer
              .withExchange(SALES_EXCHANGE, "topic")
              .withQueue(SALES_LEAD_UPDATE_QUEUE);
      rabbitMQContainer
          .withExchange(SALES_EXCHANGE, "topic")
          .withQueue(SALES_LEAD_UPDATE_QUEUE_NEW_2);
      rabbitMQContainer.withExchange(SALES_EXCHANGE, "topic").withQueue(SALES_LEAD_REASSIGN_QUEUE);
      rabbitMQContainer
          .withExchange(SALES_EXCHANGE, "topic")
          .withQueue(SALES_LEAD_REASSIGN_QUEUE_NEW);
      rabbitMQContainer
          .withExchange(WORKFLOW_EXCHANGE, "topic")
          .withQueue(LEAD_UPDATE_COMMAND_QUEUE);
      rabbitMQContainer
          .withExchange(WORKFLOW_EXCHANGE, "topic")
          .withQueue(LEAD_UPDATE_COMMAND_QUEUE_1);
      rabbitMQContainer
          .withExchange(WORKFLOW_EXCHANGE, "topic")
          .withQueue(LEAD_REASSIGN_COMMAND_QUEUE);
      rabbitMQContainer
          .withExchange(WORKFLOW_EXCHANGE, "topic")
          .withQueue(SALES_LEAD_UPDATE_QUEUE_NEW);
      rabbitMQContainer.withExchange(DEAL_EXCHANGE, "topic").withQueue(DEAL_UPDATE_QUEUE);
      rabbitMQContainer.withExchange(DEAL_EXCHANGE, "topic").withQueue(DEAL_UPDATE_QUEUE_NEW);
      rabbitMQContainer.withExchange(WORKFLOW_EXCHANGE, "topic").withQueue(DEAL_UPDATE_QUEUE);
      rabbitMQContainer.withExchange(WORKFLOW_EXCHANGE, "topic").withQueue(DEAL_UPDATE_QUEUE_NEW);
      rabbitMQContainer.withExchange(WORKFLOW_EXCHANGE, "topic").withQueue(DEAL_UPDATE_COMMAND);
      rabbitMQContainer.withExchange(DEAL_EXCHANGE, "topic").withQueue(DEAL_REASSIGN_QUEUE);
      rabbitMQContainer.withExchange(DEAL_EXCHANGE, "topic").withQueue(DEAL_REASSIGN_QUEUE_NEW);
      rabbitMQContainer
          .withExchange(WORKFLOW_EXCHANGE, "topic")
          .withQueue(DEAL_REASSIGN_COMMAND_QUEUE);
      withServices.start();

      addInlinedPropertiesToEnvironment(
          configurableApplicationContext,
          "spring.rabbitmq.port=" + rabbitMQContainer.getAmqpPort());
      addInlinedPropertiesToEnvironment(
          configurableApplicationContext,
          "spring.rabbitmq.host=" + rabbitMQContainer.getContainerIpAddress());
      addInlinedPropertiesToEnvironment(
          configurableApplicationContext,
          "spring.rabbitmq.username=" + rabbitMQContainer.getAdminUsername());
      addInlinedPropertiesToEnvironment(
          configurableApplicationContext,
          "spring.rabbitmq.password=" + rabbitMQContainer.getAdminPassword());
      addInlinedPropertiesToEnvironment(
          configurableApplicationContext, "spring.rabbitmq.virtual-host=" + "/");
    }
  }
}
