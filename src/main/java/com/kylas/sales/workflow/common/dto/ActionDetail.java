package com.kylas.sales.workflow.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kylas.sales.workflow.domain.workflow.action.task.AssignedTo;
import com.kylas.sales.workflow.domain.workflow.action.task.DueDate;
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
  class ReassignAction implements ActionDetail {

    private final Long id;
    private final String name;
  }

  @Getter
  @JsonIgnoreProperties(ignoreUnknown = true)
  @AllArgsConstructor
  class CreateTaskAction implements ActionDetail {

    private final String name;
    private final String description;
    private final Long priority;
    private final String outcome;
    private final Long type;
    private final Long status;
    private final AssignedTo assignedTo;
    private final DueDate dueDate;

  }
}
