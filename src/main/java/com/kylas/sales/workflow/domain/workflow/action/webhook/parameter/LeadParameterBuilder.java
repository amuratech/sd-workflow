package com.kylas.sales.workflow.domain.workflow.action.webhook.parameter;

import static com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.WebhookEntity.CREATED_BY;
import static com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.WebhookEntity.LEAD_OWNER;
import static com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.WebhookEntity.TENANT;
import static com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.WebhookEntity.UPDATED_BY;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import com.kylas.sales.workflow.domain.processor.EntityDetail;
import com.kylas.sales.workflow.domain.processor.lead.LeadDetail;
import com.kylas.sales.workflow.domain.service.UserService;
import com.kylas.sales.workflow.domain.user.UserDetails;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.action.webhook.WebhookAction;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class LeadParameterBuilder extends ParameterBuilder{

  @Autowired
  public LeadParameterBuilder(UserService userService) {
    super(userService);
  }

  @Override
  public boolean canBuild(EntityType entityType) {
    return EntityType.LEAD.equals(entityType);
  }

  @Override
  public Map<String, List<String>> build(WebhookAction webhookAction, EntityDetail entityDetail, String jwtToken) {
    LeadDetail lead = (LeadDetail) entityDetail;
    return Mono
        .zip(
            getUserIfRequired(webhookAction, jwtToken, LEAD_OWNER, lead.getOwnerId()),
            getUserIfRequired(webhookAction, jwtToken, CREATED_BY, lead.getCreatedBy()),
            getUserIfRequired(webhookAction, jwtToken, UPDATED_BY, lead.getUpdatedBy()),
            getTenantIfRequired(webhookAction, jwtToken)
        ).map(tuple ->
            webhookAction.getParameters().stream()
                .map(parameter -> {
                  Object entity = lead;
                  if (parameter.getEntity().equals(LEAD_OWNER)) {
                    entity = UserDetails.from(tuple.getT1());
                  } else if (parameter.getEntity().equals(CREATED_BY)) {
                    entity = UserDetails.from(tuple.getT2());
                  } else if (parameter.getEntity().equals(UPDATED_BY)) {
                    entity = UserDetails.from(tuple.getT3());
                  } else if (parameter.getEntity().equals(TENANT)) {
                    entity = tuple.getT4();
                  }
                  return new SimpleEntry<>(parameter.getName(), getParameterValue(parameter, entity));
                })
                .filter(entry -> isNotEmpty(entry.getValue()))
                .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue))
        ).block();
  }
}
