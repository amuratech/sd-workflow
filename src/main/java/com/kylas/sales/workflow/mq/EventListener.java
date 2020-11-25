package com.kylas.sales.workflow.mq;

import static com.kylas.sales.workflow.mq.RabbitMqConfig.SALES_LEAD_CREATED_QUEUE;
import static com.kylas.sales.workflow.mq.RabbitMqConfig.SALES_LEAD_UPDATED_QUEUE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylas.sales.workflow.domain.processor.WorkflowProcessor;
import com.kylas.sales.workflow.mq.event.LeadEvent;
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
  @Autowired
  public EventListener(ObjectMapper objectMapper, WorkflowProcessor workflowProcessor) {
    this.objectMapper = objectMapper;
    this.workflowProcessor = workflowProcessor;
  }

  @RabbitListener(queues = SALES_LEAD_CREATED_QUEUE)
  public void listenLeadCreatedEvent(Message message) {
    log.info("Received MessageId : {} , consumerTag: {} , Event {} ", message.getMessageProperties().getMessageId(),
        message.getMessageProperties().getConsumerTag(),
        message.getMessageProperties().getReceivedRoutingKey());
    try {
      var leadCreatedEvent = objectMapper.readValue(new String(message.getBody()), LeadEvent.class);
      workflowProcessor.process(leadCreatedEvent);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      log.error(e.getMessage(), e);
    }
  }
  @RabbitListener(queues = SALES_LEAD_UPDATED_QUEUE)
  public void listenLeadUpdatedEvent(Message message) {
    log.info("Received MessageId : {} , consumerTag: {} , Event {} ", message.getMessageProperties().getMessageId(),
        message.getMessageProperties().getConsumerTag(),
        message.getMessageProperties().getReceivedRoutingKey());
    try {
      var leadCreatedEvent = objectMapper.readValue(new String(message.getBody()), LeadEvent.class);
      workflowProcessor.process(leadCreatedEvent);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      log.error(e.getMessage(), e);
    }
  }
}
