package com.kylas.sales.workflow.mq.command;

import com.kylas.sales.workflow.domain.processor.Actionable;
import com.kylas.sales.workflow.mq.event.Metadata;
import lombok.Getter;

@Getter
public class Command {
  private Actionable entity;
  private Metadata metadata;

  public Command(Actionable entity, Metadata metadata) {
    this.entity = entity;
    this.metadata = metadata;
  }
}
