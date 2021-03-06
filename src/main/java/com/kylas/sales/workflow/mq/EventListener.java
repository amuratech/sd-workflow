package com.kylas.sales.workflow.mq;

import static com.kylas.sales.workflow.mq.RabbitMqConfig.DEAL_CREATED_QUEUE;
import static com.kylas.sales.workflow.mq.RabbitMqConfig.DEAL_UPDATED_QUEUE;
import static com.kylas.sales.workflow.mq.RabbitMqConfig.SALES_CONTACT_CREATED_QUEUE;
import static com.kylas.sales.workflow.mq.RabbitMqConfig.SALES_CONTACT_UPDATED_QUEUE;
import static com.kylas.sales.workflow.mq.RabbitMqConfig.SALES_LEAD_CREATED_QUEUE;
import static com.kylas.sales.workflow.mq.RabbitMqConfig.SALES_LEAD_UPDATED_QUEUE;
import static com.kylas.sales.workflow.mq.RabbitMqConfig.USAGE_QUEUE;
import static com.kylas.sales.workflow.mq.RabbitMqConfig.USER_NAME_UPDATED_QUEUE;
import static java.lang.String.valueOf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylas.sales.workflow.domain.WorkflowFacade;
import com.kylas.sales.workflow.domain.processor.WorkflowProcessor;
import com.kylas.sales.workflow.domain.user.UserFacade;
import com.kylas.sales.workflow.mq.event.ContactEvent;
import com.kylas.sales.workflow.mq.event.DealEvent;
import com.kylas.sales.workflow.mq.event.LeadEvent;
import com.kylas.sales.workflow.mq.event.UserNameUpdatedEvent;
import com.kylas.sales.workflow.security.InternalAuthProvider;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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
  private final WorkflowFacade workflowFacade;
  private final UserFacade userFacade;

  @Autowired
  public EventListener(ObjectMapper objectMapper, WorkflowProcessor workflowProcessor,
      InternalAuthProvider internalAuthProvider, WorkflowFacade workflowFacade, UserFacade userFacade) {
    this.objectMapper = objectMapper;
    this.workflowProcessor = workflowProcessor;
    this.internalAuthProvider = internalAuthProvider;
    this.workflowFacade = workflowFacade;
    this.userFacade = userFacade;
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

  @RabbitListener(queues = USAGE_QUEUE)
  public void listenCollectUsageEvent(Message message) {
    log.info("Received request to collect workflow usage with MessageId:{}, EventName:{} ",
        message.getMessageProperties().getMessageId(),
        message.getMessageProperties().getReceivedRoutingKey());
    workflowFacade.publishTenantUsage();
  }

  @RabbitListener(queues = USER_NAME_UPDATED_QUEUE)
  public void listenToUserNameChangedEvent(Message message) {
    log.info("Received request to update user name with MessageId:{}, EventName:{} ",
        message.getMessageProperties().getMessageId(),
        message.getMessageProperties().getReceivedRoutingKey());
    try {
      var userNameUpdatedEvent = objectMapper.readValue(new String(message.getBody()), UserNameUpdatedEvent.class);
      userFacade.tryUpdateUser(userNameUpdatedEvent.getUserId(), userNameUpdatedEvent.getTenantId(), userNameUpdatedEvent.getFirstName(),
          userNameUpdatedEvent.getLastName());
    } catch (JsonProcessingException e) {
      log.error(e.getMessage(), e);
    }
  }

  private void processLeadEvent(Message message) {
    try {
      var leadEvent = objectMapper.readValue(new String(message.getBody()), LeadEvent.class);
      var metadata = leadEvent.getMetadata();
      setMdcValues(metadata);
      internalAuthProvider.loginWith(metadata.getUserId(), metadata.getTenantId());
      workflowProcessor.process(leadEvent);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  private void setMdcValues(com.kylas.sales.workflow.mq.event.Metadata metadata) {
    MDC.put("user.tenant.id", valueOf(metadata.getTenantId()));
    MDC.put("user.id", valueOf(metadata.getUserId()));
    MDC.put("entity.type", metadata.getEntityType().name());
    MDC.put("entity.id", valueOf(metadata.getEntityId()));
  }

  private void processDealEvent(Message message) {
    try {
      var dealEvent = objectMapper.readValue(new String(message.getBody()), DealEvent.class);
      var metadata = dealEvent.getMetadata();
      internalAuthProvider.loginWith(metadata.getUserId(), metadata.getTenantId());
      setMdcValues(metadata);
      workflowProcessor.process(dealEvent);
    } catch (Exception e) {
      log.error(e.getMessage(),e);
    }
  }

  private void processContactEvent(Message message) {
    try {
      var contactCreatedEvent = objectMapper.readValue(new String(message.getBody()), ContactEvent.class);
      var metadata = contactCreatedEvent.getMetadata();
      internalAuthProvider.loginWith(metadata.getUserId(), metadata.getTenantId());
      setMdcValues(metadata);
      workflowProcessor.process(contactCreatedEvent);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }
}
