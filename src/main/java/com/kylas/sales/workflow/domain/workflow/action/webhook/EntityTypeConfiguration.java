package com.kylas.sales.workflow.domain.workflow.action.webhook;

import static com.kylas.sales.workflow.domain.workflow.EntityType.CONTACT;
import static com.kylas.sales.workflow.domain.workflow.EntityType.LEAD;
import static com.kylas.sales.workflow.domain.workflow.EntityType.USER;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.Attribute;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.ContactWebhookEntity;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.LeadWebhookEntity;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.TenantAttribute;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@Service
public class EntityTypeConfiguration {

  private final AttributeFactory attributeFactory;

  @Autowired
  public EntityTypeConfiguration(AttributeFactory attributeFactory) {
    this.attributeFactory = attributeFactory;
  }


  public Flux<EntityConfig> getConfigurations(EntityType entityType) {
    switch (entityType) {
      case LEAD:
        return Mono
            .zip(attributeFactory.getUserAttributes(), attributeFactory.getLeadAttributes())
            .map(tuples ->
                stream(attributeFactory.getEntitiesLead())
                    .map(leadWebhookEntity -> new EntityConfig(leadWebhookEntity.name(), leadWebhookEntity.getDisplayName(),
                        getAttributesForLead(tuples, leadWebhookEntity)))
                    .collect(toList()))
            .flatMapMany(Flux::fromIterable);

      case CONTACT:
        return Mono
            .zip(attributeFactory.getUserAttributes(), attributeFactory.getContactAttributes())
            .map(tuples ->
                stream(attributeFactory.getEntitiesContact())
                    .map(contactWebhookEntity -> new EntityConfig(contactWebhookEntity.name(), contactWebhookEntity.getDisplayName(),
                        getAttributesForContact(tuples, contactWebhookEntity)))
                    .collect(toList()))
            .flatMapMany(Flux::fromIterable);
    }
    return null;
  }

  private List<Attribute> getAttributesForContact(Tuple2<List<Attribute>, List<Attribute>> tuples, ContactWebhookEntity webhookEntity) {
    return webhookEntity.getType().equals(USER) ? tuples.getT1() :
        webhookEntity.getType().equals(CONTACT) ? tuples.getT2() :
            webhookEntity.getType().equals(EntityType.TENANT) ? TenantAttribute.getAttributes() : emptyList();
  }

  private List<Attribute> getAttributesForLead(Tuple2<List<Attribute>, List<Attribute>> tuples, LeadWebhookEntity webhookEntity) {
    return webhookEntity.getType().equals(USER) ? tuples.getT1() :
        webhookEntity.getType().equals(LEAD) ? tuples.getT2() :
            webhookEntity.getType().equals(EntityType.TENANT) ? TenantAttribute.getAttributes() : emptyList();
  }
}
