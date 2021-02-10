package com.kylas.sales.workflow.mq;

import com.kylas.sales.workflow.mq.event.TenantUsageEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkflowEventPublisher {

  private final RabbitTemplate rabbitTemplate;

  @Autowired
  public WorkflowEventPublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void publishTenantUsage(TenantUsageEvent event) {
    rabbitTemplate.convertAndSend("ex.workflow", TenantUsageEvent.getEventName(), event, message -> {
      message.getMessageProperties().getHeaders().remove("__TypeId__");
      return message;
    });
  }
}
