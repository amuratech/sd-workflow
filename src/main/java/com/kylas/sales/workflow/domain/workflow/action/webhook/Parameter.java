package com.kylas.sales.workflow.domain.workflow.action.webhook;

import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.WebhookEntity;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Parameter {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @NotEmpty(message = "Parameter name must not be empty.")
  private String name;
  @Enumerated(value = EnumType.STRING)
  private WebhookEntity entity;
  @NotEmpty(message = "Parameter attribute must not be empty.")
  private String attribute;

  public Parameter(@NotEmpty(message = "Parameter name must not be empty.") String name,
      WebhookEntity entity,
      @NotEmpty(message = "Parameter attribute must not be empty.") String attribute) {
    this.name = name;
    this.entity = entity;
    this.attribute = attribute;
  }
}
