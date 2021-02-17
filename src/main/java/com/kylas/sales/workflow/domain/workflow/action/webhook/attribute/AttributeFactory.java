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

  public Mono<List<Attribute>> getContactAttributes() {
    var authenticationToken = authService.getAuthenticationToken();
    return configService
        .getFields("contact", authenticationToken)
        .collectList()
        .map(entityDefinitions ->
            stream(ContactAttribute.values())
                .map(attribute -> new Attribute(attribute.name, getDisplayName(entityDefinitions, attribute.name)))
                .collect(Collectors.toList()));
  }

  public Mono<List<Attribute>> getDealAttributes() {
    var authenticationToken = authService.getAuthenticationToken();
    return configService
        .getFields("deal", authenticationToken)
        .collectList()
        .map(entityDefinitions ->
            stream(DealAttribute.values())
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

  public LeadWebhookEntity[] getEntitiesLead() {
    return LeadWebhookEntity.values();
  }

  public ContactWebhookEntity[] getEntitiesContact() {
    return ContactWebhookEntity.values();
  }

  public DealWebhookEntity[] getEntitiesDeal() {
    return DealWebhookEntity.values();
  }

  @AllArgsConstructor
  @Getter
  public enum WebhookEntity {
    CUSTOM("Custom Parameter", EntityType.CUSTOM, null),
    LEAD("Lead", EntityType.LEAD, LeadAttribute.values()),
    LEAD_OWNER("Lead Owner", EntityType.USER, UserAttribute.values()),
    CONTACT("Contact", EntityType.CONTACT, ContactAttribute.values()),
    CONTACT_OWNER("Contact Owner", EntityType.USER, UserAttribute.values()),
    CREATED_BY("Created By", EntityType.USER, UserAttribute.values()),
    UPDATED_BY("Updated By", EntityType.USER, UserAttribute.values()),
    TENANT("Tenant", EntityType.TENANT, TenantAttribute.values());


    private final String displayName;
    private final EntityType type;
    private final EntityAttribute[] entityAttributes;
  }

  @AllArgsConstructor
  @Getter
  public enum LeadWebhookEntity {
    CUSTOM("Custom Parameter", EntityType.CUSTOM, null),
    LEAD("Lead", EntityType.LEAD, LeadAttribute.values()),
    LEAD_OWNER("Lead Owner", EntityType.USER, UserAttribute.values()),
    CREATED_BY("Created By", EntityType.USER, UserAttribute.values()),
    UPDATED_BY("Updated By", EntityType.USER, UserAttribute.values()),
    TENANT("Tenant", EntityType.TENANT, TenantAttribute.values());


    private final String displayName;
    private final EntityType type;
    private final EntityAttribute[] entityAttributes;
  }

  @AllArgsConstructor
  @Getter
  public enum ContactWebhookEntity {
    CUSTOM("Custom Parameter", EntityType.CUSTOM, null),
    CONTACT("Contact", EntityType.CONTACT, ContactAttribute.values()),
    CONTACT_OWNER("Contact Owner", EntityType.USER, UserAttribute.values()),
    CREATED_BY("Created By", EntityType.USER, UserAttribute.values()),
    UPDATED_BY("Updated By", EntityType.USER, UserAttribute.values()),
    TENANT("Tenant", EntityType.TENANT, TenantAttribute.values());


    private final String displayName;
    private final EntityType type;
    private final EntityAttribute[] entityAttributes;
  }

  @AllArgsConstructor
  @Getter
  public enum DealWebhookEntity {
    CUSTOM("Custom Parameter", EntityType.CUSTOM, null),
    DEAL("Deal", EntityType.DEAL, ContactAttribute.values()),
    DEAL_OWNER("Deal Owner", EntityType.USER, UserAttribute.values()),
    CREATED_BY("Created By", EntityType.USER, UserAttribute.values()),
    UPDATED_BY("Updated By", EntityType.USER, UserAttribute.values()),
    TENANT("Tenant", EntityType.TENANT, TenantAttribute.values());


    private final String displayName;
    private final EntityType type;
    private final EntityAttribute[] entityAttributes;
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
    ACTIVE("active", "active"),
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


  @AllArgsConstructor
  @Getter
  public enum ContactAttribute implements EntityAttribute {
    ID("id", "id"),
    SALUTATION("salutation", "salutation.name"),
    FIRST_NAME("firstName", "firstName"),
    LAST_NAME("lastName", "lastName"),
    EMAILS("emails", "emails"),
    PHONE_NUMBERS("phoneNumbers", "phoneNumbers"),
    TIMEZONE("timezone", "timezone"),
    ADDRESS("address", "address"),
    CITY("city", "city"),
    STATE("state", "state"),
    COUNTRY("country", "country"),
    ZIPCODE("zipcode", "zipcode"),
    FACEBOOK("facebook", "facebook"),
    TWITTER("twitter", "twitter"),
    LINKEDIN("linkedin", "linkedin"),
    COMPANY("company", "company.name"),
    DEPARTMENT("department", "department"),
    DESIGNATION("designation", "designation"),
    CREATED_BY("createdBy", "createdBy.name"),
    CREATED_AT("createdAt", "createdAt"),
    UPDATED_BY("updatedBy", "updatedBy.name"),
    UPDATED_AT("updatedAt", "updatedAt"),
    STAKEHOLDER("stakeholder", "stakeholder"),
    DO_NOT_DISTURB("dnd", "dnd");

    private final String name;
    private final String pathToField;
  }

  @AllArgsConstructor
  @Getter
  public enum DealAttribute implements EntityAttribute {
    ID("id", "id"),
    NAME("name", "name"),
    OWNED_BY("ownedBy", "ownedBy.name"),
    ASSOCIATED_CONTACTS("associatedContacts", "associatedContacts.name"),
    ESTIMATED_CLOSURE("estimatedClosureOn", "estimatedClosureOn"),
    ESTIMATED_VALUE("estimatedValue", "estimatedValue"),
    ACTUAL_VALUE("actualValue", "actualValue"),
    PIPELINE("pipeline", "pipeline.name"),
    STATUS("pipelineStage", "pipelineStage.name"),
    PRODUCT("product", "product.name"),
    COMPANY("company", "company.name"),
    CREATED_BY("createdBy", "createdBy.name"),
    CREATED_AT("createdAt", "createdAt"),
    UPDATED_BY("updatedBy", "updatedBy.name"),
    UPDATED_AT("updatedAt", "updatedAt");

    private final String name;
    private final String pathToField;
  }

}
