package com.kylas.sales.workflow.mq.command;

import com.kylas.sales.workflow.domain.processor.Actionable;
import com.kylas.sales.workflow.mq.event.Metadata;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LeadUpdatedCommandPublisher {

  private final RabbitTemplate rabbitTemplate;

  @Autowired
  public LeadUpdatedCommandPublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void execute(Metadata metadata, Actionable lead) {

    Command command = new Command(lead, metadata);
    rabbitTemplate.convertAndSend("ex.workflow", lead.getEventName(), command, message -> {
      message.getMessageProperties().getHeaders().remove("__TypeId__");
      return message;
    });
  }
}
