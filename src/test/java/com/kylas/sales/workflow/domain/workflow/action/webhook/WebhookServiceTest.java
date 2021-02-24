package com.kylas.sales.workflow.domain.workflow.action.webhook;

import static com.kylas.sales.workflow.common.dto.ActionDetail.WebhookAction.AuthorizationType.NONE;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpMethod.GET;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylas.sales.workflow.domain.processor.lead.IdName;
import com.kylas.sales.workflow.domain.processor.lead.LeadDetail;
import com.kylas.sales.workflow.domain.service.ConfigService;
import com.kylas.sales.workflow.domain.service.UserService;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.action.webhook.parameter.ContactParameterBuilder;
import com.kylas.sales.workflow.domain.workflow.action.webhook.parameter.DealParameterBuilder;
import com.kylas.sales.workflow.domain.workflow.action.webhook.parameter.LeadParameterBuilder;
import com.kylas.sales.workflow.domain.workflow.action.webhook.parameter.ParameterBuilder;
import com.kylas.sales.workflow.security.AuthService;
import java.util.ArrayList;
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
  private EntityTypeConfiguration entityTypeConfiguration;
  @Mock
  private AuthService authService;
  @Mock
  private UserService userService;
  @Mock
  private ConfigService configService;
  @Mock
  private ExchangeFunction exchangeFunction;
  @Mock
  private CryptoService cryptoService;
  @Mock
  private ObjectMapper objectMapper;

  @BeforeEach
  void init() {
    List<ParameterBuilder> parameterBuilders = new ArrayList<>();
    parameterBuilders.add(new LeadParameterBuilder(userService,configService));
       WebClient webClient =
        WebClient.builder().exchangeFunction(exchangeFunction).build();
    webhookService = new WebhookService(entityTypeConfiguration, authService, webClient, cryptoService, objectMapper, parameterBuilders);
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
    assertDoesNotThrow(() -> webhookService.execute(action, lead, EntityType.LEAD));
  }


}