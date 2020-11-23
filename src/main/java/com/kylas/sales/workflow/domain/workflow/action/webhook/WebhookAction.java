package com.kylas.sales.workflow.domain.workflow.action.webhook;

import static com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType.WEBHOOK;

import com.kylas.sales.workflow.common.dto.ActionDetail;
import com.kylas.sales.workflow.common.dto.ActionDetail.WebhookAction.AuthorizationType;
import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.domain.processor.Actionable;
import com.kylas.sales.workflow.domain.processor.lead.Lead;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.action.AbstractWorkflowAction;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMethod;

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
  private RequestMethod method;

  @Enumerated(value = EnumType.STRING)
  private AuthorizationType authorizationType;

  @NotBlank
  private String requestUrl;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
  @JoinColumn(name = "webhook_action_id")
  private List<Parameter> parameters;

  public WebhookAction(@NotBlank String name, String description, RequestMethod method,
      AuthorizationType authorizationType, @NotBlank String requestUrl,
      List<Parameter> parameters) {
    this.name = name;
    this.description = description;
    this.method = method;
    this.authorizationType = authorizationType;
    this.requestUrl = requestUrl;
    this.parameters = parameters;
  }

  public static AbstractWorkflowAction createNew(ActionResponse action) {
    var payload = (ActionDetail.WebhookAction) action.getPayload();
    return new WebhookAction(
        payload.getName(),
        payload.getDescription(),
        payload.getMethod(),
        payload.getAuthorizationType(),
        payload.getRequestUrl(),
        payload.getParameters());
  }

  public static ActionResponse toActionResponse(WebhookAction action) {
    var webhook =
        new ActionDetail.WebhookAction(action.name, action.description,
            action.method, action.requestUrl, action.authorizationType, action.parameters);
    return new ActionResponse(action.getId(), WEBHOOK, webhook);
  }

  @Override
  public void setWorkflow(Workflow workflow) {
    super.setWorkflow(workflow);
  }

  @Override
  public Actionable process(Lead entity) {
    return () -> null;
  }

  @Override
  public WebhookAction update(ActionResponse action) {
    return null;
  }

  @Override
  public ActionType getType() {
    return WEBHOOK;
  }
}
