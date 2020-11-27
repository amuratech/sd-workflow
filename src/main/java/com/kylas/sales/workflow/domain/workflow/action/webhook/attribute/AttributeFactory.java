package com.kylas.sales.workflow.domain.workflow.action.webhook.attribute;

import com.kylas.sales.workflow.domain.entity.EntityDefinition;
import com.kylas.sales.workflow.domain.service.ConfigService;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.security.AuthService;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AttributeFactory {

  private final ConfigService configService;
  private final AuthService authService;

  @Autowired
  public AttributeFactory(ConfigService configService, AuthService authService) {
    this.configService = configService;
    this.authService = authService;
  }

  public Mono<List<Attribute>> getUserAttributes() {
    var authenticationToken = authService.getAuthenticationToken();
    return configService
        .getFields("user", authenticationToken)
        .collectList()
        .map(entityDefinitions ->
            Arrays.stream(UserAttribute.values())
                .map(attribute -> new Attribute(attribute.name, getDisplayName(entityDefinitions, attribute.name)))
                .collect(Collectors.toList()));
  }

  public Mono<List<Attribute>> getLeadAttributes() {
    var authenticationToken = authService.getAuthenticationToken();
    return configService
        .getFields("lead", authenticationToken)
        .collectList()
        .map(entityDefinitions ->
            Arrays.stream(LeadAttribute.values())
                .map(attribute -> new Attribute(attribute.name, getDisplayName(entityDefinitions, attribute.name)))
                .collect(Collectors.toList()));
  }

  public WebhookEntity[] getEntities() {
    return WebhookEntity.values();
  }

  private String getDisplayName(List<com.kylas.sales.workflow.domain.entity.EntityDefinition> entityDefinitions, String attributeName) {
    return entityDefinitions.stream()
        .filter(entityDefinition -> entityDefinition.getName().equals(attributeName))
        .findFirst()
        .map(EntityDefinition::getDisplayName)
        .orElse(StringUtils.capitalize(attributeName));
  }

  @AllArgsConstructor
  @Getter
  public enum WebhookEntity {
    LEAD("Lead", EntityType.LEAD),
    LEAD_OWNER("Lead Owner", EntityType.USER),
    CREATED_BY("Created By", EntityType.USER),
    UPDATED_BY("Updated By", EntityType.USER),
    TENANT("Tenant", EntityType.TENANT);

    private final String displayName;
    private final EntityType type;
  }

  @AllArgsConstructor
  @Getter
  public enum UserAttribute {
    FIRST_NAME("firstName"),
    LAST_NAME("lastName"),
    DESIGNATION("designation"),
    DEPARTMENT("department"),
    CURRENCY("currency"),
    TIMEZONE("timezone"),
    SIGNATURE("signature"),
    LANGUAGE("language");

    private final String name;
  }

  @AllArgsConstructor
  @Getter
  public enum LeadAttribute {
    ID("id"),
    SALUTATION("salutation.name"),
    FIRST_NAME("firstName"),
    LAST_NAME("lastName"),
    PIPELINE("pipeline.name"),
    STATUS("pipelineStage.name"),
    CREATED_BY("createdBy.name"),
    CREATED_AT("createdAt"),
    UPDATED_BY("updatedBy.name"),
    UPDATED_AT("updatedAt");

    private final String name;

  }
}
