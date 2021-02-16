package com.kylas.sales.workflow;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.context.support.TestPropertySourceUtils.addInlinedPropertiesToEnvironment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylas.sales.workflow.CreateTaskActionIntegrationTests.TestMqSetup;
import com.kylas.sales.workflow.WorkflowProcessorIntegrationTests.MockMqListener;
import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import com.kylas.sales.workflow.domain.WorkflowFacade;
import com.kylas.sales.workflow.domain.user.User;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.mq.event.ContactEvent;
import com.kylas.sales.workflow.mq.event.CreateTaskEvent;
import com.kylas.sales.workflow.mq.event.DealEvent;
import com.kylas.sales.workflow.mq.event.LeadEvent;
import com.kylas.sales.workflow.security.AuthService;
import com.kylas.sales.workflow.stubs.UserStub;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.json.JSONException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.RabbitMQContainer;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@AutoConfigureWireMock(port = 9090)
@ContextConfiguration(initializers = {TestMqSetup.class, TestDatabaseInitializer.class})
public class CreateTaskActionIntegrationTests {

  private static final String SALES_EXCHANGE = "ex.sales";
  private static final String DEAL_EXCHANGE = "ex.deal";
  private static final String WORKFLOW_EXCHANGE = "ex.workflow";
  private static final String LEAD_CREATE_TASK_QUEUE = "q.lead.task.create";
  private static final String LEAD_CREATE_TASK_UPDATE_QUEUE = "q.lead.task.create.update";
  private static final String DEAL_CREATE_TASK_QUEUE = "q.deal.task.create";
  private static final String DEAL_CREATE_TASK_UPDATE_QUEUE = "q.deal.task.create.update";
  private static final String CONTACT_CREATE_TASK_QUEUE = "q.contact.task.create";
  private static final String CONTACT_CREATE_TASK_UPDATE_QUEUE = "q.contact.task.create.update";

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
  @Sql("/test-scripts/integration/insert-lead-workflow-for-create-task-action-test.sql")
  public void givenLeadCreatedEvent_withCreateTaskAction_shouldPublishCrateTaskEvent() throws IOException, InterruptedException {
    //given
    User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("Steve");
    given(authService.getLoggedInUser()).willReturn(aUser);

    String resourceAsString = getResourceAsString("/contracts/mq/events/lead-created-event.json");
    LeadEvent leadEvent = objectMapper.readValue(resourceAsString, LeadEvent.class);
    initializeRabbitMqListener(CreateTaskEvent.getEventName(), LEAD_CREATE_TASK_QUEUE);
    //when
    rabbitTemplate.convertAndSend(SALES_EXCHANGE, LeadEvent.getLeadCreatedEventName(), leadEvent);
    //then
    mockMqListener.latch.await(3, TimeUnit.SECONDS);
    try {
      JSONAssert.assertEquals(
          getResourceAsString("/contracts/mq/task/lead-create-task-action-event-response.json"),
          mockMqListener.actualMessage,
          new CustomComparator(JSONCompareMode.STRICT, new Customization("dueDate", (o1, o2) -> true)));
    } catch (JSONException e) {
      fail(e.getMessage());
    }

    Workflow workflow = workflowFacade.get(301);
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getLastTriggeredAt()).isNotNull();
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);

  }

  @Test
  @Sql("/test-scripts/integration/insert-lead-workflow-for-create-task-action-update-test.sql")
  public void givenLeadUpdatedEvent_withCreateTaskAction_shouldPublishCrateTaskEvent() throws IOException, InterruptedException {
    //given
    User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("Steve");
    given(authService.getLoggedInUser()).willReturn(aUser);

    String resourceAsString = getResourceAsString("/contracts/mq/events/sales-lead-updated-event-payload.json");
    LeadEvent leadEvent = objectMapper.readValue(resourceAsString, LeadEvent.class);
    initializeRabbitMqListener(CreateTaskEvent.getEventName(), LEAD_CREATE_TASK_UPDATE_QUEUE);
    //when
    rabbitTemplate.convertAndSend(SALES_EXCHANGE, LeadEvent.getLeadUpdatedEventName(), leadEvent);
    //then
    mockMqListener.latch.await(3, TimeUnit.SECONDS);

    try {
      JSONAssert.assertEquals(
          getResourceAsString("/contracts/mq/task/lead-create-task-action-event-response.json"),
          mockMqListener.actualMessage,
          new CustomComparator(JSONCompareMode.STRICT, new Customization("dueDate", (o1, o2) -> true)));
    } catch (JSONException e) {
      fail(e.getMessage());
    }

    Workflow workflow = workflowFacade.get(301);
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getLastTriggeredAt()).isNotNull();
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);
  }

  @Test
  @Sql("/test-scripts/integration/insert-deal-workflow-for-create-task-action-test.sql")
  public void givenDealCreatedEvent_withCreateTaskAction_shouldPublishCrateTaskEvent() throws IOException, InterruptedException {
    //given
    User aUser = UserStub.aUser(12L, 55L, true, true, true, true, true).withName("Steve");
    given(authService.getLoggedInUser()).willReturn(aUser);

    String resourceAsString = getResourceAsString("/contracts/mq/events/deal-created-event.json");
    DealEvent dealEvent = objectMapper.readValue(resourceAsString, DealEvent.class);
    initializeRabbitMqListener(CreateTaskEvent.getEventName(), DEAL_CREATE_TASK_QUEUE);
    //when
    rabbitTemplate.convertAndSend(DEAL_EXCHANGE, DealEvent.getDealCreatedEventName(), dealEvent);
    //then
    mockMqListener.latch.await(3, TimeUnit.SECONDS);
    try {
      JSONAssert.assertEquals(
          getResourceAsString("/contracts/mq/task/deal-create-task-action-event-response.json"),
          mockMqListener.actualMessage,
          new CustomComparator(JSONCompareMode.STRICT, new Customization("dueDate", (o1, o2) -> true)));
    } catch (JSONException e) {
      fail(e.getMessage());
    }

    Workflow workflow = workflowFacade.get(301);
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getLastTriggeredAt()).isNotNull();
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);

  }

  @Test
  @Sql("/test-scripts/integration/insert-deal-workflow-for-create-task-action-update-test.sql")
  public void givenDealUpdatedEvent_withCreateTaskAction_shouldPublishCrateTaskEvent() throws IOException, InterruptedException {
    //given
    User aUser = UserStub.aUser(12L, 55L, true, true, true, true, true).withName("Steve");
    given(authService.getLoggedInUser()).willReturn(aUser);

    String resourceAsString = getResourceAsString("/contracts/mq/events/deal-updated-event.json");
    DealEvent dealEvent = objectMapper.readValue(resourceAsString, DealEvent.class);
    initializeRabbitMqListener(CreateTaskEvent.getEventName(), DEAL_CREATE_TASK_UPDATE_QUEUE);
    //when
    rabbitTemplate.convertAndSend(DEAL_EXCHANGE, DealEvent.getDealUpdatedEventName(), dealEvent);
    //then
    mockMqListener.latch.await(3, TimeUnit.SECONDS);
    try {
      JSONAssert.assertEquals(
          getResourceAsString("/contracts/mq/task/deal-create-task-action-event-response.json"),
          mockMqListener.actualMessage,
          new CustomComparator(JSONCompareMode.STRICT, new Customization("dueDate", (o1, o2) -> true)));
    } catch (JSONException e) {
      fail(e.getMessage());
    }

    Workflow workflow = workflowFacade.get(301);
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getLastTriggeredAt()).isNotNull();
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);
  }

  @Test
  @Sql("/test-scripts/integration/insert-contact-workflow-for-create-task-action-test.sql")
  public void givenContactCreatedEvent_withCreateTaskAction_shouldPublishCrateTaskEvent() throws IOException, InterruptedException {
    //given
    User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("Steve");
    given(authService.getLoggedInUser()).willReturn(aUser);

    String resourceAsString = getResourceAsString("/contracts/mq/events/contact-created-event.json");
    ContactEvent contactEvent = objectMapper.readValue(resourceAsString, ContactEvent.class);
    initializeRabbitMqListener(CreateTaskEvent.getEventName(), CONTACT_CREATE_TASK_QUEUE);
    //when
    rabbitTemplate.convertAndSend(SALES_EXCHANGE, ContactEvent.getContactCreatedEventName(), contactEvent);
    //then
    mockMqListener.latch.await(3, TimeUnit.SECONDS);
    try {
      JSONAssert.assertEquals(
          getResourceAsString("/contracts/mq/task/contact-create-task-action-event-response.json"),
          mockMqListener.actualMessage,
          new CustomComparator(JSONCompareMode.STRICT, new Customization("dueDate", (o1, o2) -> true)));
    } catch (JSONException e) {
      fail(e.getMessage());
    }

    Workflow workflow = workflowFacade.get(301);
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getLastTriggeredAt()).isNotNull();
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);

  }

  @Test
  @Sql("/test-scripts/integration/insert-contact-workflow-for-create-task-action-update-test.sql")
  public void givenContactUpdatedEvent_withCreateTaskAction_shouldPublishCrateTaskEvent() throws IOException, InterruptedException {
    //given
    User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true).withName("Steve");
    given(authService.getLoggedInUser()).willReturn(aUser);

    String resourceAsString = getResourceAsString("/contracts/mq/events/sales-contact-updated-event-payload.json");
    ContactEvent contactEvent = objectMapper.readValue(resourceAsString, ContactEvent.class);
    initializeRabbitMqListener(CreateTaskEvent.getEventName(), CONTACT_CREATE_TASK_UPDATE_QUEUE);
    //when
    rabbitTemplate.convertAndSend(SALES_EXCHANGE, ContactEvent.getContactUpdatedEventName(), contactEvent);
    //then
    mockMqListener.latch.await(3, TimeUnit.SECONDS);
    try {
      JSONAssert.assertEquals(
          getResourceAsString("/contracts/mq/task/contact-create-task-action-event-response.json"),
          mockMqListener.actualMessage,
          new CustomComparator(JSONCompareMode.STRICT, new Customization("dueDate", (o1, o2) -> true)));
    } catch (JSONException e) {
      fail(e.getMessage());
    }

    Workflow workflow = workflowFacade.get(301);
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getLastTriggeredAt()).isNotNull();
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);

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

  private String getResourceAsString(String resourcePath) throws IOException {
    var resource = new ClassPathResource(resourcePath);
    var file = resource.getFile();
    return FileUtils.readFileToString(file, "UTF-8");
  }

  @TestConfiguration
  public static class TestMqSetup
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {

      rabbitMQContainer.withExchange(SALES_EXCHANGE, "topic").withQueue(LEAD_CREATE_TASK_QUEUE);
      rabbitMQContainer.withExchange(SALES_EXCHANGE, "topic").withQueue(LEAD_CREATE_TASK_UPDATE_QUEUE);
      rabbitMQContainer.withExchange(SALES_EXCHANGE, "topic").withQueue(CONTACT_CREATE_TASK_QUEUE);
      rabbitMQContainer.withExchange(SALES_EXCHANGE, "topic").withQueue(CONTACT_CREATE_TASK_UPDATE_QUEUE);
      rabbitMQContainer.withExchange(DEAL_EXCHANGE, "topic").withQueue(DEAL_CREATE_TASK_QUEUE);
      rabbitMQContainer.withExchange(DEAL_EXCHANGE, "topic").withQueue(DEAL_CREATE_TASK_UPDATE_QUEUE);
      rabbitMQContainer.withExchange(WORKFLOW_EXCHANGE, "topic").withQueue(CreateTaskEvent.getEventName()).start();

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
