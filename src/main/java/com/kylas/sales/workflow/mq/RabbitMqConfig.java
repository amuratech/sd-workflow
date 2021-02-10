package com.kylas.sales.workflow.mq;

import static com.kylas.sales.workflow.mq.event.ContactEvent.getContactCreatedEventName;
import static com.kylas.sales.workflow.mq.event.ContactEvent.getContactUpdatedEventName;
import static com.kylas.sales.workflow.mq.event.DealEvent.getDealCreatedEventName;
import static com.kylas.sales.workflow.mq.event.DealEvent.getDealUpdatedEventName;
import static com.kylas.sales.workflow.mq.event.LeadEvent.getLeadCreatedEventName;
import static com.kylas.sales.workflow.mq.event.LeadEvent.getLeadUpdatedEventName;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

  static final String SALES_EXCHANGE = "ex.sales";
  static final String SALES_LEAD_CREATED_QUEUE = "q.lead.created.workflow";
  static final String SALES_LEAD_UPDATED_QUEUE = "q.lead.updated.workflow";
  static final String DEAL_EXCHANGE = "ex.deal";
  static final String DEAL_CREATED_QUEUE = "q.deal.created.workflow";
  static final String DEAL_UPDATED_QUEUE = "q.deal.updated.workflow";
  static final String SALES_CONTACT_CREATED_QUEUE = "q.contact.created.workflow";
  static final String SALES_CONTACT_UPDATED_QUEUE = "q.contact.updated.workflow";
  static final String SCHEDULER_EXCHANGE = "ex.scheduler";
  static final String USAGE_QUEUE = "q.collect.usage.workflow";
  static final String USAGE_REQUEST_EVENT = "scheduler.collect.usage";


  @Bean
  public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
    final var rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(producerJackson2MessageConverter());
    return rabbitTemplate;
  }

  @Bean
  public Jackson2JsonMessageConverter producerJackson2MessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public Declarables topicBindings() {
    var salesExchange = new TopicExchange(SALES_EXCHANGE, true, false);
    var dealExchange = new TopicExchange(DEAL_EXCHANGE, true, false);
    var schedulerExchange = new TopicExchange(SCHEDULER_EXCHANGE, true, false);
    var salesLeadCreatedQueue = new Queue(SALES_LEAD_CREATED_QUEUE, true);
    var salesLeadUpdatedQueue = new Queue(SALES_LEAD_UPDATED_QUEUE, true);
    var dealCreatedQueue = new Queue(DEAL_CREATED_QUEUE, true);
    var dealUpdatedQueue = new Queue(DEAL_UPDATED_QUEUE, true);
    var salesContactCreatedQueue = new Queue(SALES_CONTACT_CREATED_QUEUE, true);
    var salesContactUpdatedQueue = new Queue(SALES_CONTACT_UPDATED_QUEUE, true);
    var usageQueue = new Queue(USAGE_QUEUE, true);

    return new Declarables(
        salesLeadCreatedQueue,
        salesLeadUpdatedQueue,
        salesExchange,
        dealExchange,
        dealCreatedQueue,
        dealUpdatedQueue,
        salesContactCreatedQueue,
        salesContactUpdatedQueue,
        schedulerExchange,
        usageQueue,

        BindingBuilder.bind(usageQueue).to(schedulerExchange)
            .with(USAGE_REQUEST_EVENT),

        BindingBuilder.bind(salesLeadCreatedQueue).to(salesExchange)
            .with(getLeadCreatedEventName()),

        BindingBuilder.bind(salesLeadUpdatedQueue).to(salesExchange)
            .with(getLeadUpdatedEventName()),

        BindingBuilder.bind(dealCreatedQueue).to(dealExchange)
            .with(getDealCreatedEventName()),

        BindingBuilder.bind(dealUpdatedQueue).to(dealExchange)
            .with(getDealUpdatedEventName()),

        BindingBuilder.bind(salesContactCreatedQueue).to(salesExchange)
            .with(getContactCreatedEventName()),

        BindingBuilder.bind(salesContactUpdatedQueue).to(salesExchange)
            .with(getContactUpdatedEventName())
    );
  }
}
