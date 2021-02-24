package com.kylas.sales.workflow.domain.workflow.action.webhook.parameter;

import static com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.DealAttribute.ACTUAL_VALUE;
import static com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.DealAttribute.ESTIMATED_VALUE;
import static com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.WebhookEntity.CREATED_BY;
import static com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.WebhookEntity.DEAL;
import static com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.WebhookEntity.DEAL_OWNER;
import static com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.WebhookEntity.TENANT;
import static com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.WebhookEntity.UPDATED_BY;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import com.kylas.sales.workflow.domain.processor.EntityDetail;
import com.kylas.sales.workflow.domain.processor.deal.DealDetail;
import com.kylas.sales.workflow.domain.processor.lead.IdName;
import com.kylas.sales.workflow.domain.service.ConfigService;
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
public class DealParameterBuilder extends ParameterBuilder {

  @Autowired
  public DealParameterBuilder(UserService userService, ConfigService configService) {
    super(userService,configService);
  }

  @Override
  public boolean canBuild(EntityType entityType) {
    return EntityType.DEAL.equals(entityType);
  }

  @Override
  public Map<String, List<String>> build(WebhookAction webhookAction, EntityDetail entityDetail, String jwtToken) {
    DealDetail deal = (DealDetail) entityDetail;
    return Mono
        .zip(
            getUserIfRequired(webhookAction, jwtToken, DEAL_OWNER, deal.getOwnedBy()),
            getUserIfRequired(webhookAction, jwtToken, CREATED_BY, deal.getCreatedBy()),
            getUserIfRequired(webhookAction, jwtToken, UPDATED_BY, deal.getUpdatedBy()),
            getTenantIfRequired(webhookAction, jwtToken),
            getCurrencyIfRequired(webhookAction, jwtToken,deal.getActualValue()),
            getCurrencyIfRequired(webhookAction,jwtToken,deal.getEstimatedValue())
        ).map(tuple ->
            webhookAction.getParameters().stream()
                .map(parameter -> {
                  Object entity = deal;
                  if (parameter.getEntity().equals(DEAL_OWNER)) {
                    entity = UserDetails.from(tuple.getT1());
                  } else if (parameter.getEntity().equals(CREATED_BY)) {
                    entity = UserDetails.from(tuple.getT2());
                  } else if (parameter.getEntity().equals(UPDATED_BY)) {
                    entity = UserDetails.from(tuple.getT3());
                  } else if (parameter.getEntity().equals(TENANT)) {
                    entity = tuple.getT4();
                  } else if (parameter.getEntity().equals(DEAL) && parameter.getAttribute().equals(ACTUAL_VALUE.getName())) {
                    return new SimpleEntry<>(parameter.getName(), getFormattedActualMoneyText(deal, tuple.getT5()));
                  } else if (parameter.getEntity().equals(DEAL) && parameter.getAttribute().equals(ESTIMATED_VALUE.getName())) {
                    return new SimpleEntry<>(parameter.getName(), getFormattedEstimatedMoneyText(deal, tuple.getT6()));
                  }
                  return new SimpleEntry<>(parameter.getName(), getParameterValue(parameter, entity));
                })
                .filter(entry -> isNotEmpty(entry.getValue()))
                .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue))
        ).block();
  }

  private List<String> getFormattedActualMoneyText(DealDetail deal, IdName t5) {
    return List.of(deal.getActualValue().getValue() + " " + t5.getName());
  }
  private List<String> getFormattedEstimatedMoneyText(DealDetail deal, IdName t6) {
    return List.of(deal.getEstimatedValue().getValue() + " " + t6.getName());
  }
}
