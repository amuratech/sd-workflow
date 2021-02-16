package com.kylas.sales.workflow.mq;

import com.kylas.sales.workflow.mq.event.CreateTaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CreateTaskEventPublisher {

  private final RabbitTemplate rabbitTemplate;

  @Autowired
  public CreateTaskEventPublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void publishCreateTaskEvent(CreateTaskEvent event) {
    rabbitTemplate.convertAndSend("ex.workflow", CreateTaskEvent.getEventName(), event, message -> {
      message.getMessageProperties().getHeaders().remove("__TypeId__");
      return message;
    });
  }
}
