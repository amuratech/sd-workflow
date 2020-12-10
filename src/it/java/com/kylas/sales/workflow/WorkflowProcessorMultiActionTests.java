package com.kylas.sales.workflow;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.context.support.TestPropertySourceUtils.addInlinedPropertiesToEnvironment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylas.sales.workflow.WorkflowProcessorMultiActionTests.TestMqSetup;
import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import com.kylas.sales.workflow.domain.WorkflowFacade;
import com.kylas.sales.workflow.domain.user.User;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.mq.event.LeadEvent;
import com.kylas.sales.workflow.security.AuthService;
import com.kylas.sales.workflow.stubs.UserStub;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.RabbitMQContainer;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@AutoConfigureWireMock(port = 9090)
@ContextConfiguration(initializers = {TestMqSetup.class, TestDatabaseInitializer.class})

public class WorkflowProcessorMultiActionTests {

  static final String SALES_EXCHANGE = "ex.sales";
  static final String WORKFLOW_EXCHANGE = "ex.workflow";
  static final String SALES_LEAD_UPDATE_QUEUE = "q.workflow.lead.update.sales";
  static final String LEAD_UPDATE_COMMAND_QUEUE = "workflow.lead.update";

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

  @Test
  @Sql("/test-scripts/integration/multiple-action-lead-workflow.sql")
  public void givenLeadCreateEvent_shouldExecuteMultipleActions() throws IOException, InterruptedException, JSONException {
    //given
    User aUser = UserStub.aUser(12L, 99L, true, true, true, true, true)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    String resourceAsString = getResourceAsString("/contracts/mq/events/lead-created-v2-event.json");
    LeadEvent leadEvent = objectMapper.readValue(resourceAsString, LeadEvent.class);
    //when
    initializeRabbitMqListener(LEAD_UPDATE_COMMAND_QUEUE, SALES_LEAD_UPDATE_QUEUE);
    rabbitTemplate.convertAndSend(SALES_EXCHANGE, LeadEvent.getLeadCreatedEventName(),
        leadEvent);
    //then
    mockMqListener.latch.await(3, TimeUnit.SECONDS);
    JSONAssert
        .assertEquals(getResourceAsString("/contracts/mq/command/lead-update-patch-command-2.json"), mockMqListener.actualMessage,
            JSONCompareMode.STRICT);

    Workflow workflow = workflowFacade.get(301);
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getLastTriggeredAt()).isNotNull();
    Assertions.assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);
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
      var withSales =
          rabbitMQContainer.withExchange(SALES_EXCHANGE, "topic").withQueue(SALES_LEAD_UPDATE_QUEUE);
      rabbitMQContainer.withExchange(WORKFLOW_EXCHANGE, "topic").withQueue(LEAD_UPDATE_COMMAND_QUEUE);

      withSales.start();

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
