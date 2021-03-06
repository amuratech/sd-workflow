package com.kylas.sales.workflow.domain.workflow.action.webhook;

import static com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType.WEBHOOK;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.validator.UrlValidator.ALLOW_ALL_SCHEMES;

import com.kylas.sales.workflow.common.dto.ActionDetail;
import com.kylas.sales.workflow.common.dto.ActionDetail.WebhookAction.AuthorizationType;
import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.domain.exception.InvalidActionException;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.action.AbstractWorkflowAction;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction;
import java.util.Collections;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.UrlValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class WebhookAction extends AbstractWorkflowAction implements WorkflowAction {

  @NotBlank
  private String name;

  private String description;

  @Enumerated(value = EnumType.STRING)
  private HttpMethod method;

  @Enumerated(value = EnumType.STRING)
  private AuthorizationType authorizationType;

  private String authorizationParameter;

  @NotBlank
  private String requestUrl;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
  @JoinColumn(name = "webhook_action_id")
  @Setter(AccessLevel.NONE)
  private List<Parameter> parameters;

  public WebhookAction(@NotBlank String name, String description, HttpMethod method,
      AuthorizationType authorizationType, @NotBlank String requestUrl,
      List<Parameter> parameters, String authorizationParameter) {
    this.name = name;
    this.description = description;
    this.method = method;
    this.authorizationType = authorizationType;
    this.requestUrl = requestUrl;
    this.parameters = parameters;
    this.authorizationParameter = authorizationParameter;
  }

  public static AbstractWorkflowAction createNew(ActionResponse action) {
    return WebhookActionMapper.fromActionResponse(action);
  }

  public static ActionResponse toActionResponse(WebhookAction action) {
    return WebhookActionMapper.toActionResponse(action);
  }

  @Override
  public void setWorkflow(Workflow workflow) {
    super.setWorkflow(workflow);
  }

  public void setParameters(List<Parameter> parameters) {
    this.parameters.clear();
    if (parameters != null) {
      this.parameters.addAll(parameters);
    }
  }

  @Override
  public WebhookAction update(ActionResponse action) {
    var payload = (ActionDetail.WebhookAction) action.getPayload();
    WebhookActionMapper.validate(payload);

    this.setName(payload.getName());
    this.setDescription(payload.getDescription());
    this.setMethod(payload.getMethod());
    this.setAuthorizationType(payload.getAuthorizationType());
    this.setAuthorizationParameter(WebhookActionMapper.encrypt(payload.getAuthorizationParameter()));
    this.setRequestUrl(payload.getRequestUrl());
    this.setParameters(payload.getParameters());
    return this;
  }

  @Override
  public ActionType getType() {
    return WEBHOOK;
  }

  @Component
  private static class WebhookActionMapper {

    private static CryptoService cryptoService;
    private static final UrlValidator urlValidator = new UrlValidator(ALLOW_ALL_SCHEMES);

    @Autowired
    public void setCryptoService(CryptoService cryptoService) {
      WebhookActionMapper.cryptoService = cryptoService;
    }

    public static ActionResponse toActionResponse(WebhookAction action) {
      var authParameter = nonNull(action.authorizationParameter)
          ? cryptoService.decrypt(action.authorizationParameter)
          : null;
      var webhook = new ActionDetail.WebhookAction(action.name, action.description,
          action.method, action.requestUrl, action.authorizationType, action.parameters, authParameter);
      return new ActionResponse(action.getId(), WEBHOOK, webhook);
    }

    public static WebhookAction fromActionResponse(ActionResponse action) {
      var payload = (ActionDetail.WebhookAction) action.getPayload();
      validate(payload);
      var encrypted = encrypt(payload.getAuthorizationParameter());

      return new WebhookAction(
          payload.getName(),
          payload.getDescription(),
          payload.getMethod(),
          payload.getAuthorizationType(),
          payload.getRequestUrl(),
          payload.getParameters(),
          encrypted);
    }

    private static String encrypt(String text) {
      return nonNull(text) ? cryptoService.encrypt(text) : null;
    }

    private static void validate(ActionDetail.WebhookAction payload) {
      var keys =
          isNull(payload.getParameters()) ? Collections.emptyList()
              : payload.getParameters().stream().map(Parameter::getName).collect(toList());
      var duplicateKeyExists = keys.size() != keys.stream().distinct().count();
      if (!urlValidator.isValid(payload.getRequestUrl()) || duplicateKeyExists) {
        throw new InvalidActionException();
      }
    }
  }
}
