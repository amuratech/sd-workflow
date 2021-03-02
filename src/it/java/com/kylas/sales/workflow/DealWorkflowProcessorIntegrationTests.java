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
public class DealWorkflowProcessorIntegrationTests {

  static final String SALES_EXCHANGE = "ex.sales";
  static final String DEAL_EXCHANGE = "ex.deal";
  static final String WORKFLOW_EXCHANGE = "ex.workflow";
  static final String SALES_LEAD_UPDATE_QUEUE = "q.workflow.lead.update.sales";
  static final String SALES_LEAD_UPDATE_QUEUE_NEW = "q.workflow.lead.update.sales_new";
  static final String LEAD_UPDATE_COMMAND_QUEUE = "workflow.lead.update";
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

  @Autowired
  private ConnectionFactory connectionFactory;
  @Autowired
  private AmqpAdmin rabbitAdmin;
  @Autowired
  private RabbitTemplate rabbitTemplate;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  WorkflowFacade workflowFacade;
  @MockBean
  AuthService authService;

  @BeforeAll
  public static void initialise() {
    rabbitMQContainer.start();
  }

  @AfterAll
  public static void tearDown() {
    rabbitMQContainer.stop();
  }


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

  @Test
  @Sql("/test-scripts/integration/insert-deal-workflow-with-multiple-conditions-trigger-on-new-value.sql")
  public void givenDealCreatedEvent_withTriggerOnNewValue_shouldTriggerWorkflowConditionally()
      throws IOException, InterruptedException {
    // given
    String authenticationToken = "some-token";
    User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);
    given(authService.getAuthenticationToken()).willReturn(authenticationToken);

    String resourceAsString = getResourceAsString("/contracts/mq/events/deal-created-event-with-new-value-conditions.json");
    DealEvent dealEvent = objectMapper.readValue(resourceAsString, DealEvent.class);
    initializeRabbitMqListener(DEAL_UPDATE_COMMAND, DEAL_UPDATE_QUEUE_NEW);
    // when
    rabbitTemplate.convertAndSend(DEAL_EXCHANGE, getDealCreatedEventName(), dealEvent);
    // then
    mockMqListener.latch.await(3, TimeUnit.SECONDS);
    Workflow workflow = workflowFacade.get(301);
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getLastTriggeredAt()).isNotNull();
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);
  }

  @Test
  @Sql("/test-scripts/integration/insert-deal-workflow-with-multiple-conditions-trigger-on-new-value.sql")
  public void givenDealUpdatedEvent_withTriggerOnNewValue_shouldTriggerWorkflowConditionally()
      throws IOException, InterruptedException {
    // given
    String authenticationToken = "some-token";
    User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);
    given(authService.getAuthenticationToken()).willReturn(authenticationToken);

    String resourceAsString = getResourceAsString("/contracts/mq/events/deal-created-event-with-new-value-conditions.json");
    DealEvent dealEvent = objectMapper.readValue(resourceAsString, DealEvent.class);
    initializeRabbitMqListener(DEAL_UPDATE_COMMAND, DEAL_UPDATE_QUEUE_NEW);
    // when
    rabbitTemplate.convertAndSend(DEAL_EXCHANGE, getDealUpdatedEventName(), dealEvent);
    // then
    mockMqListener.latch.await(3, TimeUnit.SECONDS);
    Workflow workflow = workflowFacade.get(301);
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getLastTriggeredAt()).isNotNull();
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);
  }

  @Test
  @Sql("/test-scripts/integration/insert-deal-workflow-with-multiple-conditions-trigger-on-old-value.sql")
  public void givenDealCreatedEvent_withTriggerOnOldValue_shouldTriggerWorkflowConditionally()
      throws IOException, InterruptedException {
    // given
    String authenticationToken = "some-token";
    User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);
    given(authService.getAuthenticationToken()).willReturn(authenticationToken);

    String resourceAsString = getResourceAsString("/contracts/mq/events/deal-created-event-with-old-value-conditions.json");
    DealEvent dealEvent = objectMapper.readValue(resourceAsString, DealEvent.class);
    initializeRabbitMqListener(DEAL_UPDATE_COMMAND, DEAL_UPDATE_QUEUE_NEW);
    // when
    rabbitTemplate.convertAndSend(DEAL_EXCHANGE, getDealCreatedEventName(), dealEvent);
    // then
    mockMqListener.latch.await(3, TimeUnit.SECONDS);
    Workflow workflow = workflowFacade.get(301);
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getLastTriggeredAt()).isNotNull();
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);
  }

  @Test
  @Sql("/test-scripts/integration/insert-deal-workflow-with-multiple-conditions-trigger-on-old-value.sql")
  public void givenDealUpdatedEvent_withTriggerOnOldValue_shouldTriggerWorkflowConditionally()
      throws IOException, InterruptedException {
    // given
    String authenticationToken = "some-token";
    User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);
    given(authService.getAuthenticationToken()).willReturn(authenticationToken);

    String resourceAsString = getResourceAsString("/contracts/mq/events/deal-created-event-with-old-value-conditions.json");
    DealEvent dealEvent = objectMapper.readValue(resourceAsString, DealEvent.class);
    initializeRabbitMqListener(DEAL_UPDATE_COMMAND, DEAL_UPDATE_QUEUE_NEW);
    // when
    rabbitTemplate.convertAndSend(DEAL_EXCHANGE, getDealUpdatedEventName(), dealEvent);
    // then
    mockMqListener.latch.await(3, TimeUnit.SECONDS);
    Workflow workflow = workflowFacade.get(301);
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getLastTriggeredAt()).isNotNull();
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);
  }

  @Test
  @Sql("/test-scripts/integration/insert-deal-workflow-with-multiple-conditions-trigger-on-is-changed-value.sql")
  public void givenDealCreatedEvent_withTriggerOnIsChangedValue_shouldTriggerWorkflowConditionally()
      throws IOException, InterruptedException {
    // given
    String authenticationToken = "some-token";
    User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);
    given(authService.getAuthenticationToken()).willReturn(authenticationToken);

    String resourceAsString = getResourceAsString("/contracts/mq/events/deal-created-event-with-is-changed-value-conditions.json");
    DealEvent dealEvent = objectMapper.readValue(resourceAsString, DealEvent.class);
    initializeRabbitMqListener(DEAL_UPDATE_COMMAND, DEAL_UPDATE_QUEUE_NEW);
    // when
    rabbitTemplate.convertAndSend(DEAL_EXCHANGE, getDealCreatedEventName(), dealEvent);
    // then
    mockMqListener.latch.await(3, TimeUnit.SECONDS);
    Workflow workflow = workflowFacade.get(301);
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getLastTriggeredAt()).isNotNull();
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);
  }

  @Test
  @Sql("/test-scripts/integration/insert-deal-workflow-with-multiple-conditions-trigger-on-is-changed-value.sql")
  public void givenDealUpdatedEvent_withTriggerOnIsChangedValue_shouldTriggerWorkflowConditionally()
      throws IOException, InterruptedException {
    // given
    String authenticationToken = "some-token";
    User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);
    given(authService.getAuthenticationToken()).willReturn(authenticationToken);

    String resourceAsString = getResourceAsString("/contracts/mq/events/deal-created-event-with-is-changed-value-conditions.json");
    DealEvent dealEvent = objectMapper.readValue(resourceAsString, DealEvent.class);
    initializeRabbitMqListener(DEAL_UPDATE_COMMAND, DEAL_UPDATE_QUEUE_NEW);
    // when
    rabbitTemplate.convertAndSend(DEAL_EXCHANGE, getDealUpdatedEventName(), dealEvent);
    // then
    mockMqListener.latch.await(3, TimeUnit.SECONDS);
    Workflow workflow = workflowFacade.get(301);
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getLastTriggeredAt()).isNotNull();
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);
  }

  @Nested
  @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
  @AutoConfigureTestDatabase(replace = Replace.NONE)
  @ContextConfiguration(
      initializers = {
          WorkflowProcessorIntegrationTests.TestMqSetup.class,
          TestDatabaseInitializer.class
      })
  @DisplayName("Tests that execute webhooks on Deal event")
  class WebhookDealIntegrationTests {

    @Test
    @Sql("/test-scripts/create-webhook-workflow-for-deal.sql")
    public void givenDealCreateEvent_usingMethodGET_shouldExecute()
        throws IOException, InterruptedException {
      // given
      String authenticationToken = "some-token";
      User aUser = UserStub.aUser(12L, 55L, true, true, true, true, true).withName("user 1");
      given(authService.getLoggedInUser()).willReturn(aUser);
      given(authService.getAuthenticationToken()).willReturn(authenticationToken);

      stubFor(
          get("/iam/v1/users/12")
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

      String resourceAsString = getResourceAsString("/contracts/mq/events/deal-created-event.json");
      DealEvent dealEvent = objectMapper.readValue(resourceAsString, DealEvent.class);
      initializeRabbitMqListener(DEAL_UPDATE_COMMAND, DEAL_UPDATE_QUEUE);
      // when
      rabbitTemplate.convertAndSend(DEAL_EXCHANGE, DealEvent.getDealCreatedEventName(), dealEvent);
      mockMqListener.latch.await(3, TimeUnit.SECONDS);
      // then
      Workflow workflow = workflowFacade.get(301);
      Assertions.assertThat(workflow.getWorkflowExecutedEvent().getLastTriggeredAt()).isNotNull();
      Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);
    }

    @Test
    @Sql("/test-scripts/create-post-webhook-workflow-for-deal.sql")
    public void givenDealCreateEvent_usingMethodPOST_shouldExecute()
        throws IOException, InterruptedException {
      // given
      String authenticationToken = "some-token";
      User aUser = UserStub.aUser(12L, 55L, true, true, true, true, true).withName("user 1");
      given(authService.getLoggedInUser()).willReturn(aUser);
      given(authService.getAuthenticationToken()).willReturn(authenticationToken);

      stubFor(
          get("/iam/v1/users/12")
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
          getResourceAsString("/contracts/mq/events/deal-created-event.json");
      DealEvent dealEvent = objectMapper.readValue(resourceAsString, DealEvent.class);
      initializeRabbitMqListener(DEAL_UPDATE_COMMAND, DEAL_UPDATE_QUEUE);
      // when
      rabbitTemplate.convertAndSend(DEAL_EXCHANGE, DealEvent.getDealCreatedEventName(), dealEvent);
      mockMqListener.latch.await(3, TimeUnit.SECONDS);
      // then
      Workflow workflow = workflowFacade.get(301);
      Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);
    }

    @Test
    @Sql("/test-scripts/create-put-webhook-workflow-for-deal.sql")
    public void givenDealCreateEvent_usingMethodPUT_shouldExecute()
        throws IOException, InterruptedException {
      // given
      String authenticationToken = "some-token";
      User aUser = UserStub.aUser(12L, 55L, true, true, true, true, true).withName("user 1");
      given(authService.getLoggedInUser()).willReturn(aUser);
      given(authService.getAuthenticationToken()).willReturn(authenticationToken);

      stubFor(
          get("/iam/v1/users/12")
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
          getResourceAsString("/contracts/mq/events/deal-created-event.json");
      DealEvent dealEvent = objectMapper.readValue(resourceAsString, DealEvent.class);
      initializeRabbitMqListener(DEAL_UPDATE_COMMAND, DEAL_UPDATE_QUEUE);
      // when
      rabbitTemplate.convertAndSend(DEAL_EXCHANGE, DealEvent.getDealCreatedEventName(), dealEvent);
      mockMqListener.latch.await(3, TimeUnit.SECONDS);
      // then
      Workflow workflow = workflowFacade.get(301);
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
