package com.kylas.sales.workflow.domain.workflow.action.webhook;

import static java.util.Objects.isNull;

import com.kylas.sales.workflow.domain.processor.exception.WorkflowExecutionException;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.LeadAttribute;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.UserAttribute;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.WebhookEntity;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.EntityAttribute;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.TenantAttribute;
import com.kylas.sales.workflow.error.ErrorCode;
import java.util.Arrays;
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

  public String getPathToField() {
    EntityAttribute[] values =
        entity.getType().equals(EntityType.LEAD) ? LeadAttribute.values()
            : entity.getType().equals(EntityType.USER) ? UserAttribute.values() :
                entity.getType().equals(EntityType.TENANT) ? TenantAttribute.values() : null;

    return isNull(values) ? this.name
        : Arrays.stream(values)
            .filter(entityAttribute -> entityAttribute.getName().equals(attribute))
            .findFirst()
            .orElseThrow(() -> new WorkflowExecutionException(ErrorCode.INVALID_PARAMETER))
            .getPathToField();
  }
}
