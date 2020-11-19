package com.kylas.sales.workflow.domain.workflow.action.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.Attribute;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

  @InjectMocks
  private WebhookService webhookService;
  @Mock
  private AttributeFactory attributeFactory;

  @Test
  public void webhookConfig_shouldReturnConfiguration() {
    //given
    var userAttributes = List.of(new Attribute("firstName", "First Name"), new Attribute("lastName", "Last Name"));
    var leadAttributes = List.of(new Attribute("id", "Id"), new Attribute("pipeline", "Pipeline"));
    given(attributeFactory.getUserAttributes()).willReturn(Mono.just(userAttributes));
    given(attributeFactory.getLeadAttributes()).willReturn(Mono.just(leadAttributes));
    //when
    List<EntityConfig> configurations = webhookService.getConfigurations().collectList().block();

    //then
    assertThat(configurations).isNotNull().hasSize(5);
    assertThat(configurations.stream().map(EntityConfig::getEntity))
        .containsExactly("Lead", "Lead Owner", "Created By", "Updated By", "Tenant");

    var leadConfig = configurations.stream()
        .filter(config -> config.getEntity().equals("Lead")).findFirst();
    assertThat(leadConfig).isPresent();
    assertThat(leadConfig.get().getFields()).isNotEmpty();
    assertThat(leadConfig.get().getFields().stream().map(Attribute::getName))
        .containsExactly("id", "pipeline");

    var userConfig = configurations.stream()
        .filter(config -> config.getEntity().equals("Lead Owner")).findFirst();
    assertThat(userConfig).isPresent();
    assertThat(userConfig.get().getFields()).isNotEmpty();
    assertThat(userConfig.get().getFields().stream().map(Attribute::getName))
        .containsExactly("firstName", "lastName");

    var tenantConfig = configurations.stream()
        .filter(config -> config.getEntity().equals("Tenant")).findFirst();
    assertThat(tenantConfig).isPresent();
    assertThat(tenantConfig.get().getFields()).isNotEmpty();
    assertThat(tenantConfig.get().getFields().stream().map(Attribute::getName))
        .containsExactly("accountName", "industry", "address", "city", "state", "country", "zipcode",
            "language", "currency", "timezone", "companyName", "website");
  }

}