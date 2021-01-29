package com.kylas.sales.workflow.mq;

import static com.kylas.sales.workflow.mq.RabbitMqConfig.DEAL_CREATED_QUEUE;
import static com.kylas.sales.workflow.mq.RabbitMqConfig.DEAL_UPDATED_QUEUE;
import static com.kylas.sales.workflow.mq.RabbitMqConfig.SALES_CONTACT_CREATED_QUEUE;
import static com.kylas.sales.workflow.mq.RabbitMqConfig.SALES_CONTACT_UPDATED_QUEUE;
import static com.kylas.sales.workflow.mq.RabbitMqConfig.SALES_LEAD_CREATED_QUEUE;
import static com.kylas.sales.workflow.mq.RabbitMqConfig.SALES_LEAD_UPDATED_QUEUE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylas.sales.workflow.domain.processor.WorkflowProcessor;
import com.kylas.sales.workflow.mq.event.ContactEvent;
import com.kylas.sales.workflow.mq.event.DealEvent;
import com.kylas.sales.workflow.mq.event.LeadEvent;
import com.kylas.sales.workflow.security.InternalAuthProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EventListener {

  private final ObjectMapper objectMapper;
  private final WorkflowProcessor workflowProcessor;
  private final InternalAuthProvider internalAuthProvider;

  @Autowired
  public EventListener(ObjectMapper objectMapper, WorkflowProcessor workflowProcessor,
      InternalAuthProvider internalAuthProvider) {
    this.objectMapper = objectMapper;
    this.workflowProcessor = workflowProcessor;
    this.internalAuthProvider = internalAuthProvider;
  }

  @RabbitListener(queues = SALES_LEAD_CREATED_QUEUE)
  public void listenLeadCreatedEvent(Message message) {
    log.info("Received MessageId for lead created event: {} , consumerTag: {} , Event {} ", message.getMessageProperties().getMessageId(),
        message.getMessageProperties().getConsumerTag(),
        message.getMessageProperties().getReceivedRoutingKey());
    processLeadEvent(message);
  }

  @RabbitListener(queues = SALES_LEAD_UPDATED_QUEUE)
  public void listenLeadUpdatedEvent(Message message) {
    log.info("Received MessageId for lead updated event: {} , consumerTag: {} , Event {} ", message.getMessageProperties().getMessageId(),
        message.getMessageProperties().getConsumerTag(),
        message.getMessageProperties().getReceivedRoutingKey());
    processLeadEvent(message);
  }

  @RabbitListener(queues = DEAL_CREATED_QUEUE)
  public void listenDealCreatedEvent(Message message) {
    log.info("Received MessageId for deal created event: {} , consumerTag: {} , Event {} ", message.getMessageProperties().getMessageId(),
        message.getMessageProperties().getConsumerTag(),
        message.getMessageProperties().getReceivedRoutingKey());
    processDealEvent(message);
  }

  @RabbitListener(queues = DEAL_UPDATED_QUEUE)
  public void listenDealUpdatedEvent(Message message) {
    log.info("Received MessageId for deal updated event: {} , consumerTag: {} , Event {} ", message.getMessageProperties().getMessageId(),
        message.getMessageProperties().getConsumerTag(),
        message.getMessageProperties().getReceivedRoutingKey());
    processDealEvent(message);
  }

  @RabbitListener(queues = SALES_CONTACT_CREATED_QUEUE)
  public void listenContactCreatedEvent(Message message) {
    log.info("Received MessageId for contact created event: {} , consumerTag: {} , Event {} ", message.getMessageProperties().getMessageId(),
        message.getMessageProperties().getConsumerTag(),
        message.getMessageProperties().getReceivedRoutingKey());
    processContactEvent(message);
  }

  @RabbitListener(queues = SALES_CONTACT_UPDATED_QUEUE)
  public void listenContactUpdatedEvent(Message message) {
    log.info("Received MessageId for contact updated event: {} , consumerTag: {} , Event {} ", message.getMessageProperties().getMessageId(),
        message.getMessageProperties().getConsumerTag(),
        message.getMessageProperties().getReceivedRoutingKey());
    processContactEvent(message);
  }

  private void processLeadEvent(Message message) {
    try {
      var leadCreatedEvent = objectMapper.readValue(new String(message.getBody()), LeadEvent.class);
      var metadata = leadCreatedEvent.getMetadata();
      internalAuthProvider.loginWith(metadata.getUserId(), metadata.getTenantId());
      workflowProcessor.process(leadCreatedEvent);
    } catch (JsonProcessingException e) {
      log.error(e.getMessage());
    }
  }

  private void processDealEvent(Message message) {
    try {
      var dealCreatedEvent = objectMapper.readValue(new String(message.getBody()), DealEvent.class);
      var metadata = dealCreatedEvent.getMetadata();
      internalAuthProvider.loginWith(metadata.getUserId(), metadata.getTenantId());
      workflowProcessor.process(dealCreatedEvent);
    } catch (JsonProcessingException e) {
      log.error(e.getMessage());
    }
  }

  private void processContactEvent(Message message) {
    try {
      var contactCreatedEvent = objectMapper.readValue(new String(message.getBody()), ContactEvent.class);
      var metadata = contactCreatedEvent.getMetadata();
      internalAuthProvider.loginWith(metadata.getUserId(), metadata.getTenantId());
      workflowProcessor.process(contactCreatedEvent);
    } catch (JsonProcessingException e) {
      log.error(e.getMessage());
    }
  }
}
