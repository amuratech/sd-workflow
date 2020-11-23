package com.kylas.sales.workflow.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kylas.sales.workflow.domain.workflow.action.webhook.Parameter;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.bind.annotation.RequestMethod;

public interface ActionDetail {

  @Getter
  @JsonIgnoreProperties(ignoreUnknown = true)
  @AllArgsConstructor
  class WebhookAction implements ActionDetail {

    private final String name;
    private final String description;
    private final RequestMethod method;
    private final String requestUrl;
    private final AuthorizationType authorizationType;
    private final List<Parameter> parameters;

    public enum AuthorizationType {
      NONE
    }
  }

  @Getter
  @JsonIgnoreProperties(ignoreUnknown = true)
  @AllArgsConstructor
  class EditPropertyAction implements ActionDetail {

    private final String name;
    private final String value;

  }
}
