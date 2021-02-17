package com.kylas.sales.workflow.domain.workflow.action.webhook;

import static com.kylas.sales.workflow.common.dto.ActionDetail.WebhookAction.AuthorizationType.NONE;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.springframework.http.HttpMethod.GET;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylas.sales.workflow.domain.processor.EntityDetail;
import com.kylas.sales.workflow.domain.processor.exception.WorkflowExecutionException;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.action.webhook.parameter.ParameterBuilder;
import com.kylas.sales.workflow.error.ErrorCode;
import com.kylas.sales.workflow.security.AuthService;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class WebhookService {

  private static final Consumer<HttpHeaders> NO_HEADER_CONSUMER = headers -> {
  };

  private final EntityTypeConfiguration entityTypeConfiguration;
  private final AuthService authService;
  private final WebClient webClient;
  private final CryptoService cryptoService;
  private final ObjectMapper objectMapper;
  private final List<ParameterBuilder> parameterBuilders;

  @Autowired
  public WebhookService(EntityTypeConfiguration entityTypeConfiguration, AuthService authService,
      WebClient webClient, CryptoService cryptoService, ObjectMapper objectMapper,
      List<ParameterBuilder> parameterBuilders) {
    this.entityTypeConfiguration = entityTypeConfiguration;
    this.authService = authService;
    this.webClient = webClient;
    this.cryptoService = cryptoService;
    this.objectMapper = objectMapper;
    this.parameterBuilders = parameterBuilders;
  }

  public void execute(WebhookAction webhookAction, EntityDetail entity, EntityType entityType) {
    log.info("Executing webhook action with name {} & Id {}", webhookAction.getName(), webhookAction.getId());
    var requestParameters = parameterBuilders.stream()
        .filter(parameterBuilder -> parameterBuilder.canBuild(entityType))
        .findFirst()
        .map(parameterBuilder -> parameterBuilder.build(webhookAction, entity, authService.getAuthenticationToken()))
        .orElse(null);

    var uri = UriComponentsBuilder
        .fromUriString(webhookAction.getRequestUrl())
        .queryParams(buildQueryParams(webhookAction, requestParameters))
        .build()
        .toUri();
    log.info("Prepared uri is {}", uri.toString());
    webClient
        .method(webhookAction.getMethod())
        .uri(uri)
        .body(buildRequestBody(webhookAction.getMethod(), requestParameters))
        .headers(buildAuthorizationHeader(webhookAction))
        .retrieve()
        .bodyToMono(String.class)
        .subscribe(s -> log.debug("Received webhook response {}", s));
    log.info("Executed webhook action with name {} & Id {}", webhookAction.getName(), webhookAction.getId());
  }

  public Flux<EntityConfig> getConfigurations(EntityType entityType) {
    return entityTypeConfiguration.getConfigurations(entityType);

  }
  

  private BodyInserter<?, ? super ClientHttpRequest> buildRequestBody(HttpMethod method, Map<String, List<String>> parameters) {
    if (method.equals(GET)) {
      return BodyInserters.empty();
    }
    var collectedParameters = parameters.entrySet().stream()
        .filter(entry -> isNotEmpty(entry.getValue()))
        .collect(toMap(Entry::getKey, entry -> entry.getValue().size() == 1 ? entry.getValue().get(0) : entry.getValue()));
    return BodyInserters.fromValue(collectedParameters);
  }

  private LinkedMultiValueMap<String, String> buildQueryParams(WebhookAction webhookAction, Map<String, List<String>> requestParameters) {
    return new LinkedMultiValueMap<>(
        webhookAction.getMethod().equals(GET) ? requestParameters : emptyMap());
  }

  private Consumer<HttpHeaders> buildAuthorizationHeader(WebhookAction action) {
    if (action.getAuthorizationType().equals(NONE)) {
      return NO_HEADER_CONSUMER;
    }
    AuthorizationParameter auth;
    try {
      auth = objectMapper.readValue(
          Base64.getDecoder().decode(cryptoService.decrypt(action.getAuthorizationParameter())),
          AuthorizationParameter.class);
    } catch (IOException e) {
      log.error("Exception while setting authorization header for webhook action {}", action.getId(), e);
      throw new WorkflowExecutionException(ErrorCode.INVALID_PARAMETER);
    }

    return httpHeaders -> {
      switch (action.getAuthorizationType()) {
        case API_KEY:
          httpHeaders.add(auth.getKeyName(), auth.getValue());
          break;
        case BASIC_AUTH:
          httpHeaders.setBasicAuth(auth.getUsername(), auth.getPassword());
          break;
        case BEARER_TOKEN:
          httpHeaders.setBearerAuth(auth.getToken());
      }
    };
  }
}
