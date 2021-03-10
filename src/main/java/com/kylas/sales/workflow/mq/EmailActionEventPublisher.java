package com.kylas.sales.workflow.mq;

import com.kylas.sales.workflow.mq.event.EmailActionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailActionEventPublisher {

  private final RabbitTemplate rabbitTemplate;

  @Autowired
  public EmailActionEventPublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void publishEmailActionEvent(EmailActionEvent event) {
    rabbitTemplate.convertAndSend("ex.workflow", EmailActionEvent.getEventName(), event, message -> {
      message.getMessageProperties().getHeaders().remove("__TypeId__");
      return message;
    });
  }
}
