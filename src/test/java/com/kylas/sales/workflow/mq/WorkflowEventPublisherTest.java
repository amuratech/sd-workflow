package com.kylas.sales.workflow.mq;

import static org.springframework.test.context.support.TestPropertySourceUtils.addInlinedPropertiesToEnvironment;

import com.kylas.sales.workflow.common.dto.UsageRecord;
import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import com.kylas.sales.workflow.mq.WorkflowEventPublisherTest.TestMqSetup;
import com.kylas.sales.workflow.mq.event.TenantUsageEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.RabbitMQContainer;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(initializers = {TestMqSetup.class, TestDatabaseInitializer.class})
class WorkflowEventPublisherTest {

  private static final String EXCHANGE = "ex.workflow";
  private static final String QUEUE = "workflow";

  @Autowired
  private ConnectionFactory connectionFactory;
  @Autowired
  private AmqpAdmin rabbitAdmin;
  @Autowired
  private WorkflowEventPublisher workflowEventPublisher;

  private final MockMqListener mockMqListener = new MockMqListener();

  private static final RabbitMQContainer rabbitMQContainer =
      new RabbitMQContainer("rabbitmq:3.7-management-alpine");

  static class MockMqListener {

    CountDownLatch latch = new CountDownLatch(1);
    String actualMessage;

    public void receiveMessage(byte[] messageInBytes) {
      this.actualMessage = new String(messageInBytes);
    }
  }

  @BeforeAll
  public static void initialise() {
    rabbitMQContainer.start();
  }

  @AfterAll
  public static void tearDown() {
    rabbitMQContainer.stop();
  }

  @Test
  public void givenUsageDataRequest_shouldPublishIt() throws InterruptedException, IOException, JSONException {
    // given
    var container = initializeRabbitMqListener(QUEUE, EXCHANGE, TenantUsageEvent.getEventName());

    // when
    var record1 = new UsageRecord(2000L, 50L);
    var record2 = new UsageRecord(3000L, 60L);
    workflowEventPublisher.publishTenantUsage(new TenantUsageEvent(List.of(record1, record2)));

    // then
    mockMqListener.latch.await(3, TimeUnit.SECONDS);
    var expectedEventRequest = getResourceAsString("contracts/mq/events/usage-response.json");
    JSONAssert.assertEquals(expectedEventRequest, mockMqListener.actualMessage, true);
    container.stop();
  }

  private String getResourceAsString(String resourcePath) throws IOException {
    var resource = new ClassPathResource(resourcePath);
    return FileUtils.readFileToString(resource.getFile(), StandardCharsets.UTF_8);
  }

  private SimpleMessageListenerContainer initializeRabbitMqListener(String queueName, String exchangeName, String eventName) {
    rabbitAdmin.declareBinding(
        BindingBuilder.bind(new Queue(queueName))
            .to(new TopicExchange(exchangeName))
            .with(eventName));
    var listenerAdapter =
        new MessageListenerAdapter(mockMqListener, "receiveMessage");
    var container = new SimpleMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.setQueueNames(queueName);
    container.setMessageListener(listenerAdapter);
    container.start();
    return container;
  }

  @TestConfiguration
  public static class TestMqSetup
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(@NotNull ConfigurableApplicationContext configurableApplicationContext) {
      rabbitMQContainer.withExchange(EXCHANGE, "topic").withQueue(QUEUE).start();

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