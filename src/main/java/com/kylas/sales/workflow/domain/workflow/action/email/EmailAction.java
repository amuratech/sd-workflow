package com.kylas.sales.workflow.domain.workflow.action.email;

import static com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType.SEND_EMAIL;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylas.sales.workflow.common.dto.ActionDetail;
import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.domain.exception.InvalidWorkflowRequestException;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.action.AbstractWorkflowAction;
import com.kylas.sales.workflow.domain.workflow.action.ValueConverter;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class EmailAction extends AbstractWorkflowAction implements WorkflowAction {

  private long emailTemplateId;

  @NotNull
  @Column(name = "email_from")
  @Convert(converter = ValueConverter.class)
  private Object from;

  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "email_action_to_id")
  @NotEmpty
  private List<Participant> to;

  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "email_action_cc_id")
  private List<Participant> cc;

  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "email_action_bcc_id")
  private List<Participant> bcc;

  public EmailAction(long emailTemplateId, Object from, List<Participant> to,
      List<Participant> cc, List<Participant> bcc) {
    this.emailTemplateId = emailTemplateId;
    this.from = from;
    this.to = to;
    this.cc = cc;
    this.bcc = bcc;
  }

  public static AbstractWorkflowAction createNew(ActionResponse action) {
    return EmailActionMapper.fromActionResponse(action);
  }

  public static ActionResponse toActionResponse(EmailAction workflowAction) {
    return new ActionResponse(workflowAction.getId(), workflowAction.getType(), EmailActionMapper.fromWorkflowEmailAction(workflowAction));
  }

  @Override
  public AbstractWorkflowAction update(ActionResponse action) {
    var payload = (ActionDetail.EmailAction) action.getPayload();
    EmailActionMapper.validate(payload);
    this.setEmailTemplateId(payload.getEmailTemplateId());
    this.setFrom(payload.getFrom());
    this.setTo(payload.getTo());
    this.setCc(payload.getCc());
    this.setBcc(payload.getBcc());
    return this;
  }

  @Override
  public void setWorkflow(Workflow workflow) {
    super.setWorkflow(workflow);
  }

  @Override
  public ActionType getType() {
    return SEND_EMAIL;
  }

  @Component
  private static class EmailActionMapper {

    private static EmailAction fromActionResponse(ActionResponse actionResponse) {
      ActionDetail.EmailAction emailAction = (ActionDetail.EmailAction) actionResponse.getPayload();
      validate(emailAction);
      return new EmailAction(emailAction.getEmailTemplateId(), emailAction.getFrom(), emailAction.getTo(), emailAction.getCc(),
          emailAction.getBcc());
    }

    private static ActionDetail.EmailAction fromWorkflowEmailAction(EmailAction emailAction) {
      return new ActionDetail.EmailAction(emailAction.getEmailTemplateId(), emailAction.getFrom(), emailAction.getTo(),
          emailAction.getCc(),
          emailAction.getBcc());
    }

    private static void validate(ActionDetail.EmailAction payload) {
      ObjectMapper objectMapper = new ObjectMapper();

      try {
        Participant participant = objectMapper.readValue(String.valueOf(payload.getFrom()), Participant.class);
        if (!participant.getEntity().equals(EmailEntityType.USER.getEntityName())) {
          throw new InvalidWorkflowRequestException();
        }
      } catch (JsonProcessingException e) {
        log.error(e.getMessage(), e);
      }
    }
  }
}
