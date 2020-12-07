package com.kylas.sales.workflow.domain.workflow.action.webhook;

import static com.kylas.sales.workflow.common.dto.ActionDetail.WebhookAction.AuthorizationType.API_KEY;
import static com.kylas.sales.workflow.common.dto.ActionDetail.WebhookAction.AuthorizationType.BASIC_AUTH;
import static com.kylas.sales.workflow.common.dto.ActionDetail.WebhookAction.AuthorizationType.BEARER_TOKEN;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylas.sales.workflow.common.dto.ActionDetail.WebhookAction.AuthorizationType;
import com.kylas.sales.workflow.common.dto.Tenant;
import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import com.kylas.sales.workflow.domain.processor.lead.Email;
import com.kylas.sales.workflow.domain.processor.lead.EmailType;
import com.kylas.sales.workflow.domain.processor.lead.IdName;
import com.kylas.sales.workflow.domain.processor.lead.LeadDetail;
import com.kylas.sales.workflow.domain.processor.lead.PhoneNumber;
import com.kylas.sales.workflow.domain.processor.lead.PhoneType;
import com.kylas.sales.workflow.domain.service.UserService;
import com.kylas.sales.workflow.domain.user.User;
import com.kylas.sales.workflow.domain.user.User.Metadata;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.LeadAttribute;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.UserAttribute;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.WebhookEntity;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.TenantAttribute;
import com.kylas.sales.workflow.security.AuthService;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(initializers = {TestDatabaseInitializer.class})
class WebhookExecutionServiceTest {

  private WebhookService webhookService;
  @MockBean
  private AuthService authService;
  @MockBean
  private UserService userService;
  @Mock
  private ExchangeFunction exchangeFunction;
  @MockBean
  private AttributeFactory attributeFactory;
  @Autowired
  private CryptoService cryptoService;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  ResourceLoader resourceLoader;

  @BeforeEach
  void init() {
    WebClient webClient =
        WebClient.builder().exchangeFunction(exchangeFunction).build();
    webhookService = new WebhookService(attributeFactory, userService, authService, webClient, cryptoService, objectMapper);
  }

  @Test
  public void givenWorkflowRequest_withParameters_shouldCreate() {
    // given
    String authenticationToken = "some-token";
    given(authService.getAuthenticationToken()).willReturn(authenticationToken);
    given(userService.getUserDetails(eq(12L), eq(authenticationToken)))
        .willReturn(Mono.just(new User(12L, 1000L, Collections.emptySet())));
    var mockResponse = Mono.just(ClientResponse.create(HttpStatus.OK).build());
    given(exchangeFunction.exchange(any())).willReturn(mockResponse);

    // when
    LeadDetail lead = getStubbedLead(12L);
    lead.setFirstName("Tony");
    lead.setCreatedBy(new IdName(12L, "user"));
    List<Parameter> parameters = List.of(
        new Parameter("leadFirstName", WebhookEntity.LEAD, LeadAttribute.FIRST_NAME.getName()),
        new Parameter("leadCreator", WebhookEntity.LEAD, LeadAttribute.CREATED_BY.getName()));
    WebhookAction action = new WebhookAction("LeadCityWebhook", "some description", GET, AuthorizationType.NONE,
        "http://some-random-host:9000/get-webhook", parameters, null);

    //when
    webhookService.execute(action, lead);

    //then
    verify(exchangeFunction).exchange(argThat(request ->
        request.method().equals(GET) &&
            request.url().toString().equals("http://some-random-host:9000/get-webhook?leadFirstName=Tony&leadCreator=user")));
    verifyNoInteractions(userService);
  }

  @Test
  public void givenWorkflowRequest_withLeadOwnerParameters_shouldCreate() {
    // given
    String authenticationToken = "some-token";
    given(authService.getAuthenticationToken()).willReturn(authenticationToken);
    var idNameStore =
        Map.of(
            "salutation", Map.of(200L, "Mr."),
            "createdBy", Map.of(10L, "Sandeep"),
            "updatedBy", Map.of(10L, "Sandeep"));

    User user = new User(12L, 1000L, Collections.emptySet(), "Amit", "Pandit", "Amit Pandit", "dept", "des", "INR", "timezone", "lang", "AP", true,
        "some.email@kylas.io", null, 200L, 10L, 10L, new Metadata(idNameStore));
    given(userService.getUserDetails(eq(12L), eq(authenticationToken)))
        .willReturn(Mono.just(user));
    var mockResponse = Mono.just(ClientResponse.create(HttpStatus.OK).build());
    given(exchangeFunction.exchange(any())).willReturn(mockResponse);

    // when
    LeadDetail lead = getStubbedLead(12L);
    lead.setFirstName("Tony");
    lead.setCreatedBy(new IdName(12L, "user"));
    List<Parameter> parameters = List.of(
        new Parameter("leadFirstName", WebhookEntity.LEAD, LeadAttribute.FIRST_NAME.getName()),
        new Parameter("ownerFirstName", WebhookEntity.LEAD_OWNER, UserAttribute.FIRST_NAME.getName()));
    WebhookAction action = new WebhookAction("OwnerWebhook", "some description", GET, AuthorizationType.NONE,
        "http://some-random-host:9000/get-webhook", parameters, null);

    //when
    webhookService.execute(action, lead);

    //then
    verify(exchangeFunction).exchange(argThat(request ->
        request.method().equals(GET) &&
            request.url().toString().equals("http://some-random-host:9000/get-webhook?ownerFirstName=Amit&leadFirstName=Tony")));
    verify(userService, times(1))
        .getUserDetails(eq(12L), anyString());
  }

  @Test
  public void givenWorkflowRequest_withTenantParameters_shouldCreate() {
    // given
    String authenticationToken = "some-token";
    given(authService.getAuthenticationToken()).willReturn(authenticationToken);
    Tenant tenant = new Tenant();
    tenant.setAccountName("TenantAccountName");
    given(userService.getTenantDetails(eq(authenticationToken)))
        .willReturn(Mono.just(tenant));
    var mockResponse = Mono.just(ClientResponse.create(HttpStatus.OK).build());
    given(exchangeFunction.exchange(any())).willReturn(mockResponse);

    // when
    LeadDetail lead = getStubbedLead(12L);
    lead.setFirstName("Tony");
    lead.setCreatedBy(new IdName(12L, "user"));
    List<Parameter> parameters = List.of(
        new Parameter("leadFirstName", WebhookEntity.LEAD, LeadAttribute.FIRST_NAME.getName()),
        new Parameter("tenantAccountName", WebhookEntity.TENANT, TenantAttribute.ACCOUNT_NAME.getName()));
    WebhookAction action = new WebhookAction("OwnerWebhook", "some description", GET, AuthorizationType.NONE,
        "http://some-random-host:9000/get-webhook", parameters, null);

    //when
    webhookService.execute(action, lead);

    //then
    verify(exchangeFunction).exchange(argThat(request ->
        request.method().equals(GET) &&
            request.url().toString().equals("http://some-random-host:9000/get-webhook?tenantAccountName=TenantAccountName&leadFirstName=Tony")));
    verify(userService, times(1))
        .getTenantDetails(anyString());
  }

  @Test
  public void givenWorkflowRequest_withCustomParameter_shouldExecuteIt() {
    // given
    String authenticationToken = "some-token";
    given(authService.getAuthenticationToken()).willReturn(authenticationToken);
    var mockResponse = Mono.just(ClientResponse.create(HttpStatus.OK).build());
    given(exchangeFunction.exchange(any())).willReturn(mockResponse);

    // when
    LeadDetail lead = getStubbedLead(12L);
    lead.setFirstName("Tony");
    lead.setCreatedBy(new IdName(12L, "user"));
    List<Parameter> parameters = List.of(
        new Parameter("leadFirstName", WebhookEntity.LEAD, LeadAttribute.FIRST_NAME.getName()),
        new Parameter("doctorStrange", WebhookEntity.CUSTOM, "Stephen"));
    WebhookAction action = new WebhookAction("OwnerWebhook", "some description", GET, AuthorizationType.NONE,
        "http://some-random-host:9000/get-webhook", parameters, null);

    //when
    webhookService.execute(action, lead);

    //then
    verify(exchangeFunction).exchange(argThat(request ->
        request.method().equals(GET) &&
            request.url().toString().equals("http://some-random-host:9000/get-webhook?doctorStrange=Stephen&leadFirstName=Tony")));
  }

  @Test
  public void givenWorkflowRequest_withBearerToken_shouldExecuteIt() throws IOException {
    // given
    String authenticationToken = "some-token";
    given(authService.getAuthenticationToken()).willReturn(authenticationToken);
    Tenant tenant = new Tenant();
    given(userService.getTenantDetails(eq(authenticationToken)))
        .willReturn(Mono.just(tenant));
    var mockResponse = Mono.just(ClientResponse.create(HttpStatus.OK).build());
    given(exchangeFunction.exchange(any())).willReturn(mockResponse);

    // when
    LeadDetail lead = getStubbedLead(12L);
    lead.setFirstName("Tony");
    lead.setCreatedBy(new IdName(12L, "user"));
    List<Parameter> parameters = List.of(
        new Parameter("leadFirstName", WebhookEntity.LEAD, LeadAttribute.FIRST_NAME.getName()));
    var authorization = getResourceAsString("classpath:contracts/webhook/create-with-bearer-token.json");

    var authParam = cryptoService.encrypt(Base64.getEncoder().encodeToString(authorization.getBytes(UTF_8)));
    WebhookAction action = new WebhookAction("OwnerWebhook", "some description", GET, BEARER_TOKEN,
        "http://some-random-host:9000/get-webhook", parameters, authParam);

    //when
    webhookService.execute(action, lead);

    //then
    verify(exchangeFunction).exchange(argThat(request -> {
      assertThat(request.method()).isEqualTo(GET);
      assertThat(request.headers()).containsEntry(AUTHORIZATION, List.of("Bearer some-bearer-token"));
      return true;
    }));
  }

  @Test
  public void givenWorkflowRequest_withBasicAuthorization_shouldExecuteIt() throws IOException {
    // given
    String authenticationToken = "some-token";
    given(authService.getAuthenticationToken()).willReturn(authenticationToken);
    Tenant tenant = new Tenant();
    given(userService.getTenantDetails(eq(authenticationToken)))
        .willReturn(Mono.just(tenant));
    var mockResponse = Mono.just(ClientResponse.create(HttpStatus.OK).build());
    given(exchangeFunction.exchange(any())).willReturn(mockResponse);

    // when
    LeadDetail lead = getStubbedLead(12L);
    lead.setFirstName("Tony");
    lead.setCreatedBy(new IdName(12L, "user"));
    List<Parameter> parameters = List.of(
        new Parameter("leadFirstName", WebhookEntity.LEAD, LeadAttribute.FIRST_NAME.getName()));
    var authorization = getResourceAsString("classpath:contracts/webhook/create-with-basic-auth.json");

    var authParam = cryptoService.encrypt(Base64.getEncoder().encodeToString(authorization.getBytes(UTF_8)));
    WebhookAction action = new WebhookAction("OwnerWebhook", "some description", GET, BASIC_AUTH,
        "http://some-random-host:9000/get-webhook", parameters, authParam);

    //when
    webhookService.execute(action, lead);

    //then
    verify(exchangeFunction).exchange(argThat(request -> {
      assertThat(request.method()).isEqualTo(GET);
      var encoded = Base64.getEncoder().encodeToString("tony.stark:IAmIronman".getBytes(UTF_8));
      assertThat(request.headers()).containsEntry(AUTHORIZATION, List.of("Basic " + encoded));
      return true;
    }));
  }

  @Test
  public void givenWorkflowRequest_withApiKeyAuthorization_shouldExecuteIt() throws IOException {
    // given
    String authenticationToken = "some-token";
    given(authService.getAuthenticationToken()).willReturn(authenticationToken);
    Tenant tenant = new Tenant();
    given(userService.getTenantDetails(eq(authenticationToken)))
        .willReturn(Mono.just(tenant));
    var mockResponse = Mono.just(ClientResponse.create(HttpStatus.OK).build());
    given(exchangeFunction.exchange(any())).willReturn(mockResponse);

    // when
    LeadDetail lead = getStubbedLead(12L);
    lead.setFirstName("Tony");
    lead.setCreatedBy(new IdName(12L, "user"));
    List<Parameter> parameters = List.of(
        new Parameter("leadFirstName", WebhookEntity.LEAD, LeadAttribute.FIRST_NAME.getName()));
    var authorization = getResourceAsString("classpath:contracts/webhook/create-with-api-key-auth.json");

    var authParam = cryptoService.encrypt(Base64.getEncoder().encodeToString(authorization.getBytes(UTF_8)));
    WebhookAction action = new WebhookAction("OwnerWebhook", "some description", GET, API_KEY,
        "http://some-random-host:9000/get-webhook", parameters, authParam);

    //when
    webhookService.execute(action, lead);

    //then
    verify(exchangeFunction).exchange(argThat(request -> {
      assertThat(request.method()).isEqualTo(GET);
      assertThat(request.headers()).containsEntry("captain.america", List.of("JustAnotherBottleExperiment"));
      return true;
    }));
  }

  @Test
  public void givenWorkflowRequest_withCollectionParameters_shouldCreate() {
    // given
    String authenticationToken = "some-token";
    given(authService.getAuthenticationToken()).willReturn(authenticationToken);
    var mockResponse = Mono.just(ClientResponse.create(HttpStatus.OK).build());
    given(exchangeFunction.exchange(any())).willReturn(mockResponse);

    // when
    LeadDetail lead = getStubbedLead(12L);
    lead.setFirstName("Tony");
    lead.setCreatedBy(new IdName(12L, "user"));
    Email[] emails = {
        new Email(EmailType.OFFICE, "abc@kylas.io", true),
        new Email(EmailType.PERSONAL, "def@kylas.io", false)};

    PhoneNumber[] phoneNumbers = {
        new PhoneNumber(PhoneType.PERSONAL, null, "8600990099", "+91", true),
        new PhoneNumber(PhoneType.HOME, null, "8600990022", "+91", false)};

    lead.setEmails(emails);
    lead.setPhoneNumbers(phoneNumbers);
    List<Parameter> parameters = List.of(
        new Parameter("leadFirstName", WebhookEntity.LEAD, LeadAttribute.FIRST_NAME.getName()),
        new Parameter("leadEmails", WebhookEntity.LEAD, LeadAttribute.EMAILS.getName()),
        new Parameter("leadPhones", WebhookEntity.LEAD, LeadAttribute.PHONE_NUMBERS.getName()));
    WebhookAction action = new WebhookAction("OwnerWebhook", "some description", GET, AuthorizationType.NONE,
        "http://some-random-host:9000/get-webhook", parameters, null);

    //when
    webhookService.execute(action, lead);

    //then
    verify(exchangeFunction).exchange(argThat(request ->
        request.method().equals(GET) &&
            request.url().toString()
                .equals(
                    "http://some-random-host:9000/get-webhook?leadEmails=abc@kylas.io&leadEmails=def@kylas.io&leadFirstName=Tony&leadPhones=+91%208600990099&leadPhones=+91%208600990022")));
  }

  @NotNull
  private LeadDetail getStubbedLead(long userId) {
    var lead = new LeadDetail();
    lead.setOwnerId(new IdName(userId, "user"));
    lead.setCreatedBy(new IdName(userId, "user"));
    lead.setUpdatedBy(new IdName(userId, "user"));
    return lead;
  }

  private String getResourceAsString(String path) throws IOException {
    var resource = resourceLoader.getResource(path);
    return FileUtils.readFileToString(resource.getFile(), "UTF-8");
  }
}