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
  LEAD(true, true) {
    public List<String> getIdNameFields() {
      return List.of(LeadAttribute.PIPELINE.getName());
    }
  },
  CONTACT(true, true) {
    public List<String> getIdNameFields() {
      return List.of(ContactAttribute.COMPANY.getName());
    }
  },
  DEAL(true, true) {
    public List<String> getIdNameFields() {
      return emptyList();
    }
  },
  USER(false, false) {
    public List<String> getIdNameFields() {
      return emptyList();
    }
  },
  TENANT(false, false) {
    public List<String> getIdNameFields() {
      return emptyList();
    }
  },
  CUSTOM(false, false) {
    public List<String> getIdNameFields() {
      return emptyList();
    }
  };

  private final boolean workflowEntity;
  private final boolean integrationAllowed;

  public abstract List<String> getIdNameFields();
}
