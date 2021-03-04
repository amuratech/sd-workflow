package com.kylas.sales.workflow.domain.workflow.action.webhook.parameter;

import static com.kylas.sales.workflow.common.dto.condition.ExpressionField.getFieldByName;
import static com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.DealAttribute.ACTUAL_VALUE;
import static com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.DealAttribute.ESTIMATED_VALUE;
import static com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.LeadAttribute.COMPANY_PHONES;
import static com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.LeadAttribute.EMAILS;
import static com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.LeadAttribute.PHONE_NUMBERS;
import static com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.LeadAttribute.REQUIREMENT_PRODUCTS;
import static com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.WebhookEntity.DEAL;
import static com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.WebhookEntity.TENANT;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.beanutils.BeanUtils.getNestedProperty;

import com.kylas.sales.workflow.common.dto.Tenant;
import com.kylas.sales.workflow.domain.processor.EntityDetail;
import com.kylas.sales.workflow.domain.processor.contact.ContactDetail;
import com.kylas.sales.workflow.domain.processor.deal.DealDetail;
import com.kylas.sales.workflow.domain.processor.deal.Money;
import com.kylas.sales.workflow.domain.processor.exception.WorkflowExecutionException;
import com.kylas.sales.workflow.domain.processor.lead.Email;
import com.kylas.sales.workflow.domain.processor.lead.IdName;
import com.kylas.sales.workflow.domain.processor.lead.LeadDetail;
import com.kylas.sales.workflow.domain.processor.lead.PhoneNumber;
import com.kylas.sales.workflow.domain.processor.lead.Product;
import com.kylas.sales.workflow.domain.service.ConfigService;
import com.kylas.sales.workflow.domain.service.UserService;
import com.kylas.sales.workflow.domain.user.User;
import com.kylas.sales.workflow.domain.user.UserDetails;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.action.webhook.Parameter;
import com.kylas.sales.workflow.domain.workflow.action.webhook.WebhookAction;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.ContactAttribute;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.DealAttribute;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.UserAttribute;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.WebhookEntity;
import com.kylas.sales.workflow.error.ErrorCode;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.NestedNullException;
import org.apache.commons.beanutils.PropertyUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public abstract class ParameterBuilder {

  private final UserService userService;
  private final ConfigService configService;

  protected ParameterBuilder(UserService userService,ConfigService configService) {
    this.userService = userService;
    this.configService=configService;
  }
  final Mono<User> getUserIfRequired(WebhookAction action, String authToken, WebhookEntity entity, IdName user) {
    return
        action.getParameters().stream().anyMatch(parameter -> parameter.getEntity().equals(entity))
            ? userService.getUserDetails(user.getId(), authToken)
            : Mono.just(new User());
  }

  final Mono<Tenant> getTenantIfRequired(WebhookAction action, String token) {
    return
        action.getParameters().stream().anyMatch(parameter -> parameter.getEntity().equals(TENANT))
            ? userService.getTenantDetails(token)
            : Mono.just(new Tenant());
  }

  final Mono<IdName> getCurrencyIfRequired(WebhookAction action, String token, Money money) {
    if (isNull(money) || isNull(money.getCurrencyId())) {
      return Mono.just(new IdName(null,null));
    }
    Flux<IdName> currencyFlux = action.getParameters().stream().anyMatch(
        parameter -> parameter.getEntity().equals(DEAL)
            && (parameter.getAttribute().equals(ACTUAL_VALUE.getName()) || parameter.getAttribute().equals(ESTIMATED_VALUE.getName())))
        ? configService.getCurrency(List.of(money.getCurrencyId()), token)
        : Flux.just(new IdName(null,null));
    return currencyFlux.next();
  }

  final List<String> getParameterValue(Parameter parameter, Object entity) {
    EntityType type = parameter.getEntity().getType();
    String attribute = parameter.getAttribute();

    switch (type) {
      case CUSTOM:
        return List.of(parameter.getAttribute());

      case LEAD:
        if (attribute.equalsIgnoreCase(EMAILS.getName())) {
          return isNull(((LeadDetail) entity).getEmails()) ? emptyList()
              : stream(((LeadDetail) entity).getEmails())
                  .map(Email::getValue)
                  .collect(toList());
        } else if (attribute.equalsIgnoreCase(PHONE_NUMBERS.getName())) {
          return isNull(((LeadDetail) entity).getPhoneNumbers()) ? emptyList()
              : stream(((LeadDetail) entity).getPhoneNumbers())
                  .map(this::buildPhoneNumber)
                  .collect(toList());
        } else if (attribute.equalsIgnoreCase(COMPANY_PHONES.getName())) {
          return isNull(((LeadDetail) entity).getCompanyPhones()) ? emptyList()
              : stream(((LeadDetail) entity).getCompanyPhones())
                  .map(this::buildPhoneNumber)
                  .collect(toList());
        } else if (attribute.equalsIgnoreCase(REQUIREMENT_PRODUCTS.getName())) {
          return isNull(((LeadDetail) entity).getProducts()) ? emptyList()
              : ((LeadDetail) entity).getProducts().stream()
                  .map(Product::getName)
                  .collect(toList());
        }

      case USER:
        if (attribute.equalsIgnoreCase(UserAttribute.PHONE_NUMBERS.getName())) {
          return isNull(((UserDetails) entity).getPhoneNumbers()) ? emptyList()
              : stream(((UserDetails) entity).getPhoneNumbers())
                  .map(this::buildPhoneNumber)
                  .collect(toList());
        }

      case CONTACT:
        if (attribute.equalsIgnoreCase(ContactAttribute.EMAILS.getName())) {
          return isNull(((ContactDetail) entity).getEmails()) ? emptyList()
              : stream(((ContactDetail) entity).getEmails())
                  .map(Email::getValue)
                  .collect(toList());
        } else if (attribute.equalsIgnoreCase(ContactAttribute.PHONE_NUMBERS.getName())) {
          return isNull(((ContactDetail) entity).getPhoneNumbers()) ? emptyList()
              : stream(((ContactDetail) entity).getPhoneNumbers())
                  .map(this::buildPhoneNumber)
                  .collect(toList());
        }

      case DEAL:
        if (attribute.equalsIgnoreCase(DealAttribute.ASSOCIATED_CONTACTS.getName())) {
          return isNull(((DealDetail) entity).getAssociatedContacts()) ? emptyList()
              : ((DealDetail) entity).getAssociatedContacts().stream()
                  .map(IdName::getName)
                  .collect(toList());
        }
    }
    var property = getPropertyValue(entity,parameter);
    return List.of(nonNull(property) ? property.toString() : "");
  }
  private String getPropertyValue(Object entity, Parameter parameter){
    String actualValue = null;
      try {
        actualValue = getNestedProperty(entity, parameter.fetchPathToField());
      } catch (NestedNullException ignored) {
      } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
        log.error("Exception occurred while getting actual value for {}", parameter.getName());
      }
      return actualValue;
  }

  private String buildPhoneNumber(PhoneNumber phoneNumber) {
    return String.format("%s %s", phoneNumber.getDialCode(), phoneNumber.getValue());
  }
  public abstract boolean canBuild(EntityType entityType);
  public abstract Map<String, List<String>> build(WebhookAction webhookAction, EntityDetail entityDetail,String jwtToken);
}
