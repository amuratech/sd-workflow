package com.kylas.sales.workflow.domain.workflow.action.webhook;

import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.Attribute;
import java.util.List;
import lombok.Getter;

@Getter
public class EntityConfig {

  private final String entity;
  private final String entityDisplayName;
  private final List<Attribute> fields;

  public EntityConfig(String entity, String entityDisplayName, List<Attribute> fields) {
    this.entity = entity;
    this.entityDisplayName = entityDisplayName;
    this.fields = fields;
  }
}
