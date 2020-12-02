package com.kylas.sales.workflow.mq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.springframework.test.context.support.TestPropertySourceUtils.addInlinedPropertiesToEnvironment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import com.kylas.sales.workflow.domain.processor.WorkflowProcessor;
import com.kylas.sales.workflow.mq.EventListenerTest.TestMqSetup;
import com.kylas.sales.workflow.mq.event.EntityAction;
import com.kylas.sales.workflow.mq.event.LeadEvent;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.RabbitMQContainer;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(initializers = {TestMqSetup.class, TestDatabaseInitializer.class})
class EventListenerTest {

  private static RabbitMQContainer rabbitMQContainer =
      new RabbitMQContainer("rabbitmq:3.7-management-alpine");

  @Autowired
  private RabbitTemplate rabbitTemplate;
  @Autowired
  private ObjectMapper objectMapper;
  @MockBean
  private WorkflowProcessor workflowProcessor;
  @Captor
  ArgumentCaptor<LeadEvent> leadCreatedEventArgumentCaptor;

  CountDownLatch latch = new CountDownLatch(1);

  @BeforeAll
  public static void initialise() {
    rabbitMQContainer.start();
  }

  @AfterAll
  public static void tearDown() {
    rabbitMQContainer.stop();
  }

  @Test
  public void givenLeadCreatedRequest_shouldCaptureIt() throws IOException, InterruptedException {
    //given
    String resourceAsString = getResourceAsString("/contracts/mq/events/lead-created-event.json");
    LeadEvent leadEvent = objectMapper.readValue(resourceAsString, LeadEvent.class);
    doNothing().when(workflowProcessor).process(Mockito.any(LeadEvent.class));
    //when
    rabbitTemplate.convertAndSend(RabbitMqConfig.SALES_EXCHANGE, LeadEvent.getLeadCreatedEventName(),
        leadEvent);
    //then
    latch.await(3, TimeUnit.SECONDS);
    Mockito.verify(workflowProcessor, times(1)).process(leadCreatedEventArgumentCaptor.capture());
    LeadEvent eventReceived = leadCreatedEventArgumentCaptor.getValue();
    assertThat(eventReceived.getMetadata().getTenantId()).isEqualTo(99L);
    assertThat(eventReceived.getMetadata().getEntityAction()).isEqualTo(EntityAction.CREATED);


  }

  @Test
  public void givenLeadUpdatedRequest_shouldCaptureIt() throws Exception {
    //given
    String resourceAsString = getResourceAsString("/contracts/mq/events/lead-updated-event.json");
    LeadEvent leadEvent = objectMapper.readValue(resourceAsString, LeadEvent.class);
    doNothing().when(workflowProcessor).process(Mockito.any(LeadEvent.class));
    //when
    rabbitTemplate.convertAndSend(RabbitMqConfig.SALES_EXCHANGE, LeadEvent.getLeadUpdatedEventName(),
        leadEvent);
    //then
    latch.await(3, TimeUnit.SECONDS);
    Mockito.verify(workflowProcessor, times(1)).process(leadCreatedEventArgumentCaptor.capture());
    LeadEvent eventReceived = leadCreatedEventArgumentCaptor.getValue();
    assertThat(eventReceived.getEntity().getOwnerId().getId()).isEqualTo(55);
    assertThat(eventReceived.getOldEntity().getSalutation().getId()).isEqualTo(473);
    assertThat(eventReceived.getMetadata().getTenantId()).isEqualTo(99L);
    assertThat(eventReceived.getMetadata().getEntityAction()).isEqualTo(EntityAction.UPDATED);

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
      var withSales =
          rabbitMQContainer.withExchange(RabbitMqConfig.SALES_EXCHANGE, "topic");//.withQueue(RabbitMqConfig.SALES_LEAD_CREATED_QUEUE);
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