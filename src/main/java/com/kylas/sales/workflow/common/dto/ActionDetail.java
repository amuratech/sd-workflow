package com.kylas.sales.workflow.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.kylas.sales.workflow.domain.workflow.action.webhook.Parameter;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpMethod;

public interface ActionDetail {

  @Getter
  @JsonIgnoreProperties(ignoreUnknown = true)
  @AllArgsConstructor
  class WebhookAction implements ActionDetail {

    private final String name;
    private final String description;
    private final HttpMethod method;
    private final String requestUrl;
    private final AuthorizationType authorizationType;
    private final List<Parameter> parameters;
    private final String authorizationParameter;

    public enum AuthorizationType {
      NONE, API_KEY, BEARER_TOKEN, BASIC_AUTH
    }
  }

  @Getter
  @JsonIgnoreProperties(ignoreUnknown = true)
  @AllArgsConstructor
  class EditPropertyAction implements ActionDetail {

    private final String name;
    private final Object value;
    private final ValueType valueType;

    public enum ValueType {
      ARRAY, OBJECT, PLAIN,
    }
  }

  @Getter
  @JsonIgnoreProperties(ignoreUnknown = true)
  @AllArgsConstructor
  @JsonInclude(Include.NON_NULL)
  class ReassignAction implements ActionDetail {
    
    private String name;
    private final Long ownerId;

    public ReassignAction(Long ownerId) {
      this.ownerId = ownerId;
    }
  }
}
