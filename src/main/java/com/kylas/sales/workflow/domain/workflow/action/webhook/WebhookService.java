package com.kylas.sales.workflow.domain.workflow.action.webhook;

import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.TenantAttribute;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class WebhookService {

  private final AttributeFactory attributeFactory;

  @Autowired
  public WebhookService(AttributeFactory attributeFactory) {
    this.attributeFactory = attributeFactory;
  }

  public Flux<EntityConfig> getConfigurations() {
    return Mono
        .zip(attributeFactory.getUserAttributes(), attributeFactory.getLeadAttributes())
        .map(tuples ->
            List.of(
                new EntityConfig("Lead", tuples.getT2()),
                new EntityConfig("Lead Owner", tuples.getT1()),
                new EntityConfig("Created By", tuples.getT1()),
                new EntityConfig("Updated By", tuples.getT1()),
                new EntityConfig("Tenant", TenantAttribute.getAttributes())))
        .flatMapMany(Flux::fromIterable);
  }
}
