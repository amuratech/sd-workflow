package com.kylas.sales.workflow.domain.workflow.action.webhook;

import static com.kylas.sales.workflow.domain.workflow.EntityType.LEAD;
import static com.kylas.sales.workflow.domain.workflow.EntityType.USER;
import static com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.LeadAttribute.COMPANY_PHONES;
import static com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.LeadAttribute.EMAILS;
import static com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.LeadAttribute.PHONE_NUMBERS;
import static com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.LeadAttribute.REQUIREMENT_PRODUCTS;
import static com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.WebhookEntity.CREATED_BY;
import static com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.WebhookEntity.LEAD_OWNER;
import static com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.WebhookEntity.TENANT;
import static com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.WebhookEntity.UPDATED_BY;
import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

import com.kylas.sales.workflow.common.dto.Tenant;
import com.kylas.sales.workflow.domain.processor.exception.WorkflowExecutionException;
import com.kylas.sales.workflow.domain.processor.lead.Email;
import com.kylas.sales.workflow.domain.processor.lead.IdName;
import com.kylas.sales.workflow.domain.processor.lead.LeadDetail;
import com.kylas.sales.workflow.domain.processor.lead.PhoneNumber;
import com.kylas.sales.workflow.domain.processor.lead.Product;
import com.kylas.sales.workflow.domain.service.UserService;
import com.kylas.sales.workflow.domain.user.User;
import com.kylas.sales.workflow.domain.user.UserDetails;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.Attribute;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.UserAttribute;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.WebhookEntity;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.TenantAttribute;
import com.kylas.sales.workflow.error.ErrorCode;
import com.kylas.sales.workflow.security.AuthService;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@Service
@Slf4j
public class WebhookService {

  private final AttributeFactory attributeFactory;
  private final UserService userService;
  private final AuthService authService;
  private final WebClient webClient;

  @Autowired
  public WebhookService(AttributeFactory attributeFactory, UserService userService, AuthService authService,
      WebClient webClient) {
    this.attributeFactory = attributeFactory;
    this.userService = userService;
    this.authService = authService;
    this.webClient = webClient;
  }

  public Flux<EntityConfig> getConfigurations() {
    return Mono
        .zip(attributeFactory.getUserAttributes(), attributeFactory.getLeadAttributes())
        .map(tuples ->
            stream(attributeFactory.getEntities())
                .map(webhookEntity -> new EntityConfig(webhookEntity.name(), webhookEntity.getDisplayName(), getAttributesFor(tuples, webhookEntity)))
                .collect(toList()))
        .flatMapMany(Flux::fromIterable);
  }

  private List<Attribute> getAttributesFor(Tuple2<List<Attribute>, List<Attribute>> tuples, WebhookEntity webhookEntity) {
    return webhookEntity.getType().equals(USER) ? tuples.getT1() :
        webhookEntity.getType().equals(LEAD) ? tuples.getT2() :
            TenantAttribute.getAttributes();
  }

  public void execute(WebhookAction webhookAction, LeadDetail entity) {
    log.info("Executing webhook action with name {} & Id {}", webhookAction.getName(), webhookAction.getId());
    var uri = UriComponentsBuilder
        .fromUriString(webhookAction.getRequestUrl())
        .queryParams(new LinkedMultiValueMap<>(buildLeadParameters(webhookAction, entity)))
        .build()
        .toUri();
    log.info("Prepared uri is {}", uri.toString());
    webClient
        .method(webhookAction.getMethod())
        .uri(uri)
        .retrieve()
        .bodyToMono(String.class)
        .subscribe(s -> log.debug("Received webhook response {}", s));
    log.info("Executed webhook action with name {} & Id {}", webhookAction.getName(), webhookAction.getId());
  }

  private Map<String, List<String>> buildLeadParameters(WebhookAction webhookAction, LeadDetail lead) {
    String token = authService.getAuthenticationToken();
    return Mono
        .zip(
            getUserIfRequired(webhookAction, token, LEAD_OWNER, lead.getOwnerId()),
            getUserIfRequired(webhookAction, token, CREATED_BY, lead.getCreatedBy()),
            getUserIfRequired(webhookAction, token, UPDATED_BY, lead.getUpdatedBy()),
            getTenantIfRequired(webhookAction, token)
        ).map(tuple ->
            webhookAction.getParameters().stream()
                .map(parameter -> {
                  Object entity = lead;
                  if (parameter.getEntity().equals(LEAD_OWNER)) {
                    entity = UserDetails.from(tuple.getT1());
                  } else if (parameter.getEntity().equals(CREATED_BY)) {
                    entity = UserDetails.from(tuple.getT2());
                  } else if (parameter.getEntity().equals(UPDATED_BY)) {
                    entity = UserDetails.from(tuple.getT3());
                  } else if (parameter.getEntity().equals(TENANT)) {
                    entity = tuple.getT4();
                  }
                  return new SimpleEntry<>(parameter.getName(), getParameterValue(parameter, entity));
                })
                .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue))
        ).block();
  }

  private Mono<Tenant> getTenantIfRequired(WebhookAction action, String token) {
    return
        action.getParameters().stream().anyMatch(parameter -> parameter.getEntity().equals(TENANT))
            ? userService.getTenantDetails(token)
            : Mono.just(new Tenant());
  }

  private Mono<User> getUserIfRequired(WebhookAction action, String authToken, WebhookEntity entity, IdName user) {
    return
        action.getParameters().stream().anyMatch(parameter -> parameter.getEntity().equals(entity))
            ? userService.getUserDetails(user.getId(), authToken)
            : Mono.just(new User());
  }

  private List<String> getParameterValue(Parameter parameter, Object entity) {
    try {
      EntityType type = parameter.getEntity().getType();
      String attribute = parameter.getAttribute();
      if (type.equals(LEAD) && attribute.equalsIgnoreCase(EMAILS.getName())) {
        return stream(((LeadDetail) entity).getEmails())
            .map(Email::getValue)
            .collect(toList());
      } else if (type.equals(LEAD) && attribute.equalsIgnoreCase(PHONE_NUMBERS.getName())) {
        return stream(((LeadDetail) entity).getPhoneNumbers())
            .map(this::buildPhoneNumber)
            .collect(toList());
      } else if (type.equals(LEAD) && attribute.equalsIgnoreCase(COMPANY_PHONES.getName())) {
        return stream(((LeadDetail) entity).getCompanyPhones())
            .map(this::buildPhoneNumber)
            .collect(toList());
      } else if (type.equals(LEAD) && attribute.equalsIgnoreCase(REQUIREMENT_PRODUCTS.getName())) {
        return ((LeadDetail) entity).getProducts().stream()
            .map(Product::getName)
            .collect(toList());
      } else if (type.equals(USER) && attribute.equalsIgnoreCase(UserAttribute.PHONE_NUMBERS.getName())) {
        return stream(((UserDetails) entity).getPhoneNumbers())
            .map(this::buildPhoneNumber)
            .collect(toList());
      }
      var property = PropertyUtils.getNestedProperty(entity, parameter.getPathToField());
      return List.of(nonNull(property) ? property.toString() : "");
    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
      log.error("Exception while building webhook request to be executed.", e);
      throw new WorkflowExecutionException(ErrorCode.INVALID_PARAMETER);
    }
  }

  private String buildPhoneNumber(PhoneNumber phoneNumber) {
    return String.format("%s %s", phoneNumber.getDialCode(), phoneNumber.getValue());
  }

}
