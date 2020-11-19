package com.kylas.sales.workflow.domain.workflow.action.webhook.attribute;

import com.kylas.sales.workflow.domain.entity.EntityDefinition;
import com.kylas.sales.workflow.domain.service.ConfigService;
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

  private String getDisplayName(List<com.kylas.sales.workflow.domain.entity.EntityDefinition> entityDefinitions, String attributeName) {
    return entityDefinitions.stream()
        .filter(entityDefinition -> entityDefinition.getName().equals(attributeName))
        .findFirst()
        .map(EntityDefinition::getDisplayName)
        .orElse(StringUtils.capitalize(attributeName));
  }

  @AllArgsConstructor
  @Getter
  public enum UserAttribute {
    SALUTATION("salutation"),
    FIRST_NAME("firstName"),
    LAST_NAME("lastName"),
    EMAIL("email"),
    PHONE("phoneNumbers"),
    DESIGNATION("designation"),
    DEPARTMENT("department"),
    CURRENCY("currency"),
    TIMEZONE("timezone"),
    SIGNATURE("signature"),
    LANGUAGE("language"),
    ACTIVE_STATUS("active"),
    CREATED_BY("createdBy"),
    UPDATED_BY("updatedBy");

    private final String name;
  }

  @AllArgsConstructor
  @Getter
  public enum LeadAttribute {
    ID("id"),
    SALUTATION("salutation"),
    FIRST_NAME("firstName"),
    LAST_NAME("lastName"),
    PIPELINE("pipeline"),
    STATUS("pipelineStage"),
    CREATED_BY("createdBy"),
    CREATED_AT("createdAt"),
    UPDATED_BY("updatedBy"),
    UPDATED_AT("updatedAt");

    private final String name;

  }
}
