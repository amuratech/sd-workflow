package com.kylas.sales.workflow.domain.workflow.action.webhook;

import static com.kylas.sales.workflow.common.dto.ActionDetail.WebhookAction.AuthorizationType.NONE;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpMethod.GET;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylas.sales.workflow.domain.processor.lead.IdName;
import com.kylas.sales.workflow.domain.processor.lead.LeadDetail;
import com.kylas.sales.workflow.domain.service.UserService;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.Attribute;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory;
import com.kylas.sales.workflow.security.AuthService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

  @InjectMocks
  private WebhookService webhookService;
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
    WebClient webClient =
        WebClient.builder().exchangeFunction(exchangeFunction).build();
    webhookService = new WebhookService(attributeFactory, userService, authService, webClient, cryptoService, objectMapper);
  }

  @Test
  public void webhookConfig_shouldReturnConfiguration() {
    //given
    var userAttributes = List.of(new Attribute("firstName", "First Name"), new Attribute("lastName", "Last Name"));
    var leadAttributes = List.of(new Attribute("id", "Id"), new Attribute("pipeline", "Pipeline"));
    var contactAttributes=List.of(new Attribute("id", "Id"),new Attribute("address", "Address"));
    given(attributeFactory.getUserAttributes()).willReturn(Mono.just(userAttributes));
    given(attributeFactory.getLeadAttributes()).willReturn(Mono.just(leadAttributes));
    given(attributeFactory.getContactAttributes()).willReturn(Mono.just(contactAttributes));
    given(attributeFactory.getEntities()).willCallRealMethod();
    //when
    List<EntityConfig> configurations = webhookService.getConfigurations().collectList().block();

    //then
    assertThat(configurations).isNotNull().hasSize(7);
    assertThat(configurations.stream().map(EntityConfig::getEntityDisplayName))
        .containsExactly("Custom Parameter", "Lead", "Contact", "Lead Owner", "Created By", "Updated By", "Tenant");

    assertThat(configurations.stream().map(EntityConfig::getEntity))
        .containsExactly("CUSTOM", "LEAD", "CONTACT", "LEAD_OWNER", "CREATED_BY", "UPDATED_BY", "TENANT");

    var leadConfig = configurations.stream()
        .filter(config -> config.getEntityDisplayName().equals("Lead")).findFirst();
    assertThat(leadConfig).isPresent();
    assertThat(leadConfig.get().getFields()).isNotEmpty();
    assertThat(leadConfig.get().getFields().stream().map(Attribute::getName))
        .containsExactly("id", "pipeline");

    var contactConfig = configurations.stream()
        .filter(config -> config.getEntityDisplayName().equals("Contact")).findFirst();
    assertThat(contactConfig).isPresent();
    assertThat(contactConfig.get().getFields()).isNotEmpty();
    assertThat(contactConfig.get().getFields().stream().map(Attribute::getName))
        .containsExactly("id", "address");

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
  public void webhookAction_withoutParameters_shouldBeExecuted() {
    //given
    given(authService.getAuthenticationToken())
        .willReturn("some-token");
    given(exchangeFunction.exchange(any())).willReturn(Mono.just(ClientResponse.create(HttpStatus.OK).build()));

    var lead = new LeadDetail();
    lead.setCity("Pune");
    lead.setOwnerId(new IdName(1000L, "user"));
    lead.setCreatedBy(new IdName(1000L, "user"));
    lead.setUpdatedBy(new IdName(1000L, "user"));
    var action = new WebhookAction("LeadCityWebhook", "desc", GET, NONE, "https://reqres.in/api/users", emptyList(), null);
    action.setId(UUID.randomUUID());

    //when
    //then
    assertDoesNotThrow(() -> webhookService.execute(action, lead));
  }

}