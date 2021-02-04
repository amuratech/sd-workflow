package com.kylas.sales.workflow.domain.workflow;

import static java.util.Collections.emptyList;

import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.ContactAttribute;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.LeadAttribute;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EntityType {
  LEAD(true) {
    public List<String> getIdNameFields() {
      return List.of(LeadAttribute.PIPELINE.name());
    }
  },
  CONTACT(true) {
    public List<String> getIdNameFields() {
      return List.of(ContactAttribute.COMPANY.name());
    }
  },
  DEAL(true) {
    public List<String> getIdNameFields() {
      return emptyList();
    }
  },
  USER(false) {
    public List<String> getIdNameFields() {
      return emptyList();
    }
  },
  TENANT(false) {
    public List<String> getIdNameFields() {
      return emptyList();
    }
  },
  CUSTOM(false) {
    public List<String> getIdNameFields() {
      return emptyList();
    }
  };

  private final boolean workflowEntity;

  public abstract List<String> getIdNameFields();
}
