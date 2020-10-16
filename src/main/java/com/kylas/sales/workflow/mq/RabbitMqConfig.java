package com.kylas.sales.workflow.mq;

import com.kylas.sales.workflow.mq.event.LeadCreatedEvent;
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

    var salesLeadCreatedQueue = new Queue(SALES_LEAD_CREATED_QUEUE, true);

    return new Declarables(
        salesLeadCreatedQueue,
        salesExchange,

        BindingBuilder.bind(salesLeadCreatedQueue).to(salesExchange)
            .with(LeadCreatedEvent.getEventName())
    );
  }
}
