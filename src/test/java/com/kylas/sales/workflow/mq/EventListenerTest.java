package com.kylas.sales.workflow.mq;

import static com.kylas.sales.workflow.domain.workflow.EntityType.DEAL;
import static com.kylas.sales.workflow.mq.RabbitMqConfig.DEAL_EXCHANGE;
import static com.kylas.sales.workflow.mq.RabbitMqConfig.SALES_EXCHANGE;
import static com.kylas.sales.workflow.mq.event.DealEvent.getDealCreatedEventName;
import static com.kylas.sales.workflow.mq.event.DealEvent.getDealUpdatedEventName;
import static com.kylas.sales.workflow.mq.event.EntityAction.CREATED;
import static com.kylas.sales.workflow.mq.event.EntityAction.UPDATED;
import static com.kylas.sales.workflow.mq.event.LeadEvent.getLeadCreatedEventName;
import static com.kylas.sales.workflow.mq.event.LeadEvent.getLeadUpdatedEventName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.context.support.TestPropertySourceUtils.addInlinedPropertiesToEnvironment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import com.kylas.sales.workflow.domain.processor.WorkflowProcessor;
import com.kylas.sales.workflow.domain.processor.deal.DealDetail;
import com.kylas.sales.workflow.mq.EventListenerTest.TestMqSetup;
import com.kylas.sales.workflow.mq.event.DealEvent;
import com.kylas.sales.workflow.mq.event.LeadEvent;
import com.kylas.sales.workflow.mq.event.Metadata;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
  ArgumentCaptor<LeadEvent> leadEventArgumentCaptor;
  @Captor
  ArgumentCaptor<DealEvent> dealEventArgumentCaptor;

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
    doNothing().when(workflowProcessor).process(any(LeadEvent.class));
    //when
    rabbitTemplate.convertAndSend(SALES_EXCHANGE, getLeadCreatedEventName(),
        leadEvent);
    //then
    latch.await(3, TimeUnit.SECONDS);
    verify(workflowProcessor, times(1)).process(leadEventArgumentCaptor.capture());
    LeadEvent eventReceived = leadEventArgumentCaptor.getValue();
    assertThat(eventReceived.getMetadata().getTenantId()).isEqualTo(99L);
    assertThat(eventReceived.getMetadata().getEntityAction()).isEqualTo(CREATED);


  }

  @Test
  public void givenLeadUpdatedRequest_shouldCaptureIt() throws Exception {
    //given
    String resourceAsString = getResourceAsString("/contracts/mq/events/lead-updated-event.json");
    LeadEvent leadEvent = objectMapper.readValue(resourceAsString, LeadEvent.class);
    doNothing().when(workflowProcessor).process(any(LeadEvent.class));
    //when
    rabbitTemplate.convertAndSend(SALES_EXCHANGE, getLeadUpdatedEventName(),
        leadEvent);
    //then
    latch.await(3, TimeUnit.SECONDS);
    verify(workflowProcessor, times(1)).process(leadEventArgumentCaptor.capture());
    LeadEvent eventReceived = leadEventArgumentCaptor.getValue();
    assertThat(eventReceived.getEntity().getOwnerId().getId()).isEqualTo(55);
    assertThat(eventReceived.getOldEntity().getSalutation().getId()).isEqualTo(473);
    assertThat(eventReceived.getMetadata().getTenantId()).isEqualTo(99L);
    assertThat(eventReceived.getMetadata().getEntityAction()).isEqualTo(UPDATED);

  }

  @Test
  public void givenDealCreatedEventRequest_shouldCaptureIt() throws IOException, InterruptedException {
    //given
    String resourceAsString = getResourceAsString("/contracts/mq/events/deal-created-event.json");
    DealEvent dealEvent = objectMapper.readValue(resourceAsString, DealEvent.class);
    doNothing().when(workflowProcessor).process(any(DealEvent.class));
    //when
    rabbitTemplate.convertAndSend(DEAL_EXCHANGE, getDealCreatedEventName(), dealEvent);
    //then
    latch.await(3, TimeUnit.SECONDS);
    verify(workflowProcessor, times(1)).process(dealEventArgumentCaptor.capture());
    DealDetail entity = dealEventArgumentCaptor.getValue().getEntity();
    assertThat(entity.getId()).isEqualTo(999L);
    assertThat(entity.getOwnedBy().getId()).isEqualTo(12L);
    assertThat(entity.getOwnedBy().getName()).isEqualTo("James Bond");
    assertThat(entity.getName()).isEqualTo("new deal");
    assertThat(entity.getEstimatedValue().getCurrencyId()).isEqualTo(1L);
    assertThat(entity.getEstimatedValue().getValue()).isEqualTo(1000);
    assertThat(entity.getActualValue().getCurrencyId()).isEqualTo(2L);
    assertThat(entity.getActualValue().getValue()).isEqualTo(2000);
    assertThat(entity.getEstimatedClosureOn()).isBeforeOrEqualTo(new Date());
    assertThat(entity.getProduct().getId()).isEqualTo(2L);
    assertThat(entity.getProduct().getName()).isEqualTo("Marketing Service");
    assertThat(entity.getPipeline().getId()).isEqualTo(122L);
    assertThat(entity.getPipeline().getName()).isEqualTo("Demo");
    assertThat(entity.getPipeline().getStage().getId()).isEqualTo(1L);
    assertThat(entity.getPipeline().getStage().getName()).isEqualTo("Open");
    assertThat(entity.getCompany().getId()).isEqualTo(15L);
    assertThat(entity.getCompany().getName()).isEqualTo("Dell");
    assertThat(entity.getAssociatedContacts()).hasSize(1);
    assertThat(entity.getAssociatedContacts().get(0).getId()).isEqualTo(14L);
    assertThat(entity.getAssociatedContacts().get(0).getName()).isEqualTo("Tony Stark");

    assertThat(dealEventArgumentCaptor.getValue().getOldEntity()).isNull();

    Metadata metadata = dealEventArgumentCaptor.getValue().getMetadata();

    assertThat(metadata.getEntityId()).isEqualTo(999L);
    assertThat(metadata.getEntityAction()).isEqualTo(CREATED);
    assertThat(metadata.getEntityType()).isEqualTo(DEAL);
    assertThat(metadata.getTenantId()).isEqualTo(55L);
    assertThat(metadata.getUserId()).isEqualTo(12L);
    assertThat(metadata.getWorkflowId()).isNull();
    assertThat(metadata.getExecutedWorkflows()).isEmpty();
  }

  @Test
  public void givenDealUpdatedEventRequest_shouldCaptureIt() throws IOException, InterruptedException {
    //given
    String resourceAsString = getResourceAsString("/contracts/mq/events/deal-updated-event.json");
    DealEvent dealEvent = objectMapper.readValue(resourceAsString, DealEvent.class);
    doNothing().when(workflowProcessor).process(any(DealEvent.class));
    //when
    rabbitTemplate.convertAndSend(DEAL_EXCHANGE, getDealUpdatedEventName(), dealEvent);
    //then
    latch.await(3, TimeUnit.SECONDS);
    verify(workflowProcessor, times(1)).process(dealEventArgumentCaptor.capture());
    DealDetail entity = dealEventArgumentCaptor.getValue().getEntity();
    assertThat(entity.getId()).isEqualTo(999L);
    assertThat(entity.getOwnedBy().getId()).isEqualTo(12L);
    assertThat(entity.getOwnedBy().getName()).isEqualTo("James Bond");
    assertThat(entity.getName()).isEqualTo("new deal");
    assertThat(entity.getEstimatedValue().getCurrencyId()).isEqualTo(1L);
    assertThat(entity.getEstimatedValue().getValue()).isEqualTo(1000);
    assertThat(entity.getActualValue().getCurrencyId()).isEqualTo(2L);
    assertThat(entity.getActualValue().getValue()).isEqualTo(2000);
    assertThat(entity.getEstimatedClosureOn()).isBeforeOrEqualTo(new Date());
    assertThat(entity.getProduct().getId()).isEqualTo(2L);
    assertThat(entity.getProduct().getName()).isEqualTo("Marketing Service");
    assertThat(entity.getPipeline().getId()).isEqualTo(11L);
    assertThat(entity.getPipeline().getName()).isEqualTo("Test Deal Pipeline");
    assertThat(entity.getPipeline().getStage().getId()).isEqualTo(1L);
    assertThat(entity.getPipeline().getStage().getName()).isEqualTo("Open");
    assertThat(entity.getCompany().getId()).isEqualTo(13L);
    assertThat(entity.getCompany().getName()).isEqualTo("Dell enterprises");
    assertThat(entity.getAssociatedContacts()).hasSize(1);
    assertThat(entity.getAssociatedContacts().get(0).getId()).isEqualTo(12L);
    assertThat(entity.getAssociatedContacts().get(0).getName()).isEqualTo("Tony Stark");

    DealDetail oldEntity = dealEventArgumentCaptor.getValue().getOldEntity();
    assertThat(oldEntity.getId()).isEqualTo(999L);
    assertThat(oldEntity.getOwnedBy().getId()).isEqualTo(11L);
    assertThat(oldEntity.getOwnedBy().getName()).isEqualTo("James BondOld");
    assertThat(oldEntity.getName()).isEqualTo("old deal");
    assertThat(oldEntity.getEstimatedValue().getCurrencyId()).isEqualTo(2L);
    assertThat(oldEntity.getEstimatedValue().getValue()).isEqualTo(10000);
    assertThat(oldEntity.getActualValue().getCurrencyId()).isEqualTo(1L);
    assertThat(oldEntity.getActualValue().getValue()).isEqualTo(20000);
    assertThat(oldEntity.getEstimatedClosureOn()).isBeforeOrEqualTo(new Date());
    assertThat(oldEntity.getProduct().getId()).isEqualTo(1L);
    assertThat(oldEntity.getProduct().getName()).isEqualTo("Marketing Service Old");
    assertThat(oldEntity.getPipeline().getId()).isEqualTo(10L);
    assertThat(oldEntity.getPipeline().getName()).isEqualTo("Test Deal Pipeline Old");
    assertThat(oldEntity.getPipeline().getStage().getId()).isEqualTo(2L);
    assertThat(oldEntity.getPipeline().getStage().getName()).isEqualTo("Open");
    assertThat(oldEntity.getCompany().getId()).isEqualTo(12L);
    assertThat(oldEntity.getCompany().getName()).isEqualTo("Dell enterprises Old");
    assertThat(oldEntity.getAssociatedContacts()).hasSize(1);
    assertThat(oldEntity.getAssociatedContacts().get(0).getId()).isEqualTo(11L);
    assertThat(oldEntity.getAssociatedContacts().get(0).getName()).isEqualTo("Tony StarkOld");

    Metadata metadata = dealEventArgumentCaptor.getValue().getMetadata();

    assertThat(metadata.getEntityId()).isEqualTo(999L);
    assertThat(metadata.getEntityAction()).isEqualTo(UPDATED);
    assertThat(metadata.getEntityType()).isEqualTo(DEAL);
    assertThat(metadata.getTenantId()).isEqualTo(55L);
    assertThat(metadata.getUserId()).isEqualTo(12L);
    assertThat(metadata.getWorkflowId()).isNull();
    assertThat(metadata.getExecutedWorkflows()).isEmpty();
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
          rabbitMQContainer.withExchange(SALES_EXCHANGE, "topic");//.withQueue(RabbitMqConfig.SALES_LEAD_CREATED_QUEUE);
      withSales.start();

      var withDeal = rabbitMQContainer.withExchange(DEAL_EXCHANGE, "topic");
      withDeal.start();

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