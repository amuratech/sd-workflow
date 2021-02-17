package com.kylas.sales.workflow.domain.workflow.action.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylas.sales.workflow.domain.service.UserService;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.Attribute;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory;
import com.kylas.sales.workflow.security.AuthService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class EntityTypeConfigurationTest {

  @InjectMocks
  private EntityTypeConfiguration entityTypeConfiguration;
  @Mock
  private AttributeFactory attributeFactory;
  @Mock
  private AuthService authService;
  @Mock
  private UserService userService;
  @Mock
  private ExchangeFunction exchangeFunction;
  @Mock
  private CryptoService cryptoService;
  @Mock
  private ObjectMapper objectMapper;

  @BeforeEach
  void init() {
    entityTypeConfiguration = new EntityTypeConfiguration(attributeFactory);
  }

  @Test
  public void webhookConfigLead_shouldReturnConfiguration() {
    //given
    var userAttributes = List.of(new Attribute("firstName", "First Name"), new Attribute("lastName", "Last Name"));
    var leadAttributes = List.of(new Attribute("id", "Id"), new Attribute("pipeline", "Pipeline"));
    given(attributeFactory.getUserAttributes()).willReturn(Mono.just(userAttributes));
    given(attributeFactory.getLeadAttributes()).willReturn(Mono.just(leadAttributes));
    given(attributeFactory.getEntitiesLead()).willCallRealMethod();
    //when
    List<EntityConfig> configurations = entityTypeConfiguration.getConfigurations(EntityType.LEAD).collectList().block();

    //then
    assertThat(configurations).isNotNull().hasSize(6);
    assertThat(configurations.stream().map(EntityConfig::getEntityDisplayName))
        .containsExactly("Custom Parameter", "Lead", "Lead Owner", "Created By", "Updated By", "Tenant");

    assertThat(configurations.stream().map(EntityConfig::getEntity))
        .containsExactly("CUSTOM", "LEAD", "LEAD_OWNER", "CREATED_BY", "UPDATED_BY", "TENANT");

    var leadConfig = configurations.stream()
        .filter(config -> config.getEntityDisplayName().equals("Lead")).findFirst();
    assertThat(leadConfig).isPresent();
    assertThat(leadConfig.get().getFields()).isNotEmpty();
    assertThat(leadConfig.get().getFields().stream().map(Attribute::getName))
        .containsExactly("id", "pipeline");

    var userConfig = configurations.stream()
        .filter(config -> config.getEntityDisplayName().equals("Lead Owner")).findFirst();
    assertThat(userConfig).isPresent();
    assertThat(userConfig.get().getFields()).isNotEmpty();
    assertThat(userConfig.get().getFields().stream().map(Attribute::getName))
        .containsExactly("firstName", "lastName");

    var tenantConfig = configurations.stream()
        .filter(config -> config.getEntityDisplayName().equals("Tenant")).findFirst();
    assertThat(tenantConfig).isPresent();
    assertThat(tenantConfig.get().getFields()).isNotEmpty();
    assertThat(tenantConfig.get().getFields().stream().map(Attribute::getName))
        .containsExactly("accountName", "industry", "address", "city", "state", "country", "zipcode",
            "language", "currency", "timezone", "companyName", "website");
  }

  @Test
  public void webhookConfigContact_shouldReturnConfiguration() {
    //given
    var userAttributes = List.of(new Attribute("firstName", "First Name"), new Attribute("lastName", "Last Name"));
    var contactAttributes = List.of(new Attribute("id", "Id"), new Attribute("address", "Address"));
    given(attributeFactory.getUserAttributes()).willReturn(Mono.just(userAttributes));
    given(attributeFactory.getContactAttributes()).willReturn(Mono.just(contactAttributes));
    given(attributeFactory.getEntitiesContact()).willCallRealMethod();
    //when
    List<EntityConfig> configurations = entityTypeConfiguration.getConfigurations(EntityType.CONTACT).collectList().block();

    //then
    assertThat(configurations).isNotNull().hasSize(6);
    assertThat(configurations.stream().map(EntityConfig::getEntityDisplayName))
        .containsExactly("Custom Parameter", "Contact", "Contact Owner", "Created By", "Updated By", "Tenant");

    assertThat(configurations.stream().map(EntityConfig::getEntity))
        .containsExactly("CUSTOM", "CONTACT", "CONTACT_OWNER", "CREATED_BY", "UPDATED_BY", "TENANT");

    var contactConfig = configurations.stream()
        .filter(config -> config.getEntityDisplayName().equals("Contact")).findFirst();
    assertThat(contactConfig).isPresent();
    assertThat(contactConfig.get().getFields()).isNotEmpty();
    assertThat(contactConfig.get().getFields().stream().map(Attribute::getName))
        .containsExactly("id", "address");

    var tenantConfig = configurations.stream()
        .filter(config -> config.getEntityDisplayName().equals("Tenant")).findFirst();
    assertThat(tenantConfig).isPresent();
    assertThat(tenantConfig.get().getFields()).isNotEmpty();
    assertThat(tenantConfig.get().getFields().stream().map(Attribute::getName))
        .containsExactly("accountName", "industry", "address", "city", "state", "country", "zipcode",
            "language", "currency", "timezone", "companyName", "website");
  }

  @Test
  public void webhookConfigDeal_shouldReturnConfiguration() {
    //given
    var userAttributes = List.of(new Attribute("firstName", "First Name"), new Attribute("lastName", "Last Name"));
    var dealAttribute = List.of(new Attribute("id", "Id"), new Attribute("name", "Name"));
    given(attributeFactory.getUserAttributes()).willReturn(Mono.just(userAttributes));
    given(attributeFactory.getDealAttributes()).willReturn(Mono.just(dealAttribute));
    given(attributeFactory.getEntitiesDeal()).willCallRealMethod();
    //when
    List<EntityConfig> configurations = entityTypeConfiguration.getConfigurations(EntityType.DEAL).collectList().block();

    //then
    assertThat(configurations).isNotNull().hasSize(6);
    assertThat(configurations.stream().map(EntityConfig::getEntityDisplayName))
        .containsExactly("Custom Parameter", "Deal", "Deal Owner", "Created By", "Updated By", "Tenant");

    assertThat(configurations.stream().map(EntityConfig::getEntity))
        .containsExactly("CUSTOM", "DEAL", "DEAL_OWNER", "CREATED_BY", "UPDATED_BY", "TENANT");

    var dealConfig = configurations.stream()
        .filter(config -> config.getEntityDisplayName().equals("Deal")).findFirst();
    assertThat(dealConfig).isPresent();
    assertThat(dealConfig.get().getFields()).isNotEmpty();
    assertThat(dealConfig.get().getFields().stream().map(Attribute::getName))
        .containsExactly("id", "name");

    var tenantConfig = configurations.stream()
        .filter(config -> config.getEntityDisplayName().equals("Tenant")).findFirst();
    assertThat(tenantConfig).isPresent();
    assertThat(tenantConfig.get().getFields()).isNotEmpty();
    assertThat(tenantConfig.get().getFields().stream().map(Attribute::getName))
        .containsExactly("accountName", "industry", "address", "city", "state", "country", "zipcode",
            "language", "currency", "timezone", "companyName", "website");
  }
}