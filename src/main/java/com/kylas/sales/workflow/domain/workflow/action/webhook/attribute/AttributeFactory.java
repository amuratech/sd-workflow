package com.kylas.sales.workflow.domain.workflow.action.webhook.attribute;

import static java.util.Arrays.stream;

import com.kylas.sales.workflow.domain.entity.EntityDefinition;
import com.kylas.sales.workflow.domain.service.ConfigService;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.security.AuthService;
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
            stream(UserAttribute.values())
                .map(attribute -> new Attribute(attribute.name, getDisplayName(entityDefinitions, attribute.name)))
                .collect(Collectors.toList()));
  }

  public Mono<List<Attribute>> getLeadAttributes() {
    var authenticationToken = authService.getAuthenticationToken();
    return configService
        .getFields("lead", authenticationToken)
        .collectList()
        .map(entityDefinitions ->
            stream(LeadAttribute.values())
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
    CUSTOM("Custom Parameter", EntityType.CUSTOM),
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
  public enum UserAttribute implements EntityAttribute {
    SALUTATION("salutation", "salutation.name"),
    FIRST_NAME("firstName", "firstName"),
    LAST_NAME("lastName", "lastName"),
    EMAIL("email", "email"),
    PHONE_NUMBERS("phoneNumbers", "phoneNumbers"),
    DESIGNATION("designation", "designation"),
    DEPARTMENT("department", "department"),
    CURRENCY("currency", "currency"),
    TIMEZONE("timezone", "timezone"),
    SIGNATURE("signature", "signature"),
    LANGUAGE("language", "language"),
    CREATED_BY("createdBy", "createdBy.name"),
    UPDATED_BY("updatedBy", "updatedBy.name");

    private final String name;
    private final String pathToField;
  }

  @AllArgsConstructor
  @Getter
  public enum LeadAttribute implements EntityAttribute {
    ID("id", "id"),
    SALUTATION("salutation", "salutation.name"),
    FIRST_NAME("firstName", "firstName"),
    LAST_NAME("lastName", "lastName"),
    EMAILS("emails", "emails"),
    PHONE_NUMBERS("phoneNumbers", "phoneNumbers"),
    PIPELINE("pipeline", "pipeline.name"),
    STATUS("pipelineStage", "pipelineStage.name"),
    TIMEZONE("timezone", "timezone"),
    ADDRESS("address", "address"),
    CITY("city", "city"),
    STATE("state", "state"),
    COUNTRY("country", "country"),
    ZIPCODE("zipcode", "zipcode"),
    FACEBOOK("facebook", "facebook"),
    TWITTER("twitter", "twitter"),
    LINKEDIN("linkedIn", "linkedIn"),
    COMPANY_NAME("companyName", "companyName"),
    DEPARTMENT("department", "department"),
    DESIGNATION("designation", "designation"),
    COMPANY_INDUSTRY("companyIndustry", "companyIndustry"),
    COMPANY_BUSINESS_TYPE("companyBusinessType", "companyBusinessType"),
    COMPANY_EMPLOYEES("companyEmployees", "companyEmployees"),
    COMPANY_ANNUAL_REVENUE("companyAnnualRevenue", "companyAnnualRevenue"),
    COMPANY_WEBSITE("companyWebsite", "companyWebsite"),
    COMPANY_PHONES("companyPhones", "companyPhones"),
    REQUIREMENT_NAME("requirementName", "requirementName"),
    REQUIREMENT_PRODUCTS("products", "products"),
    REQUIREMENT_CURRENCY("requirementCurrency", "requirementCurrency"),
    REQUIREMENT_BUDGET("requirementBudget", "requirementBudget"),
    CAMPAIGN("campaign", "campaign.name"),
    SOURCE("source", "source.name"),
    CREATED_BY("createdBy", "createdBy.name"),
    CREATED_AT("createdAt", "createdAt"),
    UPDATED_BY("updatedBy", "updatedBy.name"),
    UPDATED_AT("updatedAt", "updatedAt");

    private final String name;
    private final String pathToField;
  }
}
