package com.kylas.sales.workflow.domain.workflow.action;

import static com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType.EDIT_PROPERTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.kylas.sales.workflow.common.dto.ActionDetail;
import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.domain.exception.InvalidActionException;
import com.kylas.sales.workflow.domain.processor.Actionable;
import com.kylas.sales.workflow.domain.processor.exception.WorkflowExecutionException;
import com.kylas.sales.workflow.domain.processor.lead.Lead;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.error.ErrorCode;
import javax.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class EditPropertyAction extends AbstractWorkflowAction implements com.kylas.sales.workflow.domain.workflow.action.WorkflowAction {

  private String name;
  private String value;

  private EditPropertyAction(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public static AbstractWorkflowAction createNew(ActionResponse actionResponse) {
    var payload = (ActionDetail.EditPropertyAction) actionResponse.getPayload();
    if (isBlank(payload.getName()) || isBlank(payload.getValue())) {
      throw new InvalidActionException();
    }
    return new EditPropertyAction(payload.getName(), payload.getValue());
  }

  public static ActionResponse toActionResponse(EditPropertyAction action) {
    var editPropertyAction = new ActionDetail.EditPropertyAction(action.name, action.value);
    return new ActionResponse(action.getId(), EDIT_PROPERTY, editPropertyAction);
  }

  @Override
  public void setWorkflow(Workflow workflow) {
    super.setWorkflow(workflow);
  }

  @Override
  public Actionable process(Lead entity) {
    try {
      log.info("Executing EditPropertyAction with Id {}, name {} and value {} ", getId(), name, value);
      EvaluationContext context = SimpleEvaluationContext.forReadWriteDataBinding().build();
      ExpressionParser parser = new SpelExpressionParser();
      parser.parseExpression(name).setValue(context, entity, value);
      return entity;
    } catch (SpelEvaluationException e) {
      log.error("Exception for EditPropertyAction with Id {}, name {} and value {} with errorMessage {} ", getId(), name, value, e.getMessageCode());
      log.error(e.getMessage(), e);
      throw new WorkflowExecutionException(ErrorCode.UPDATE_PROPERTY);
    }
  }

  @Override
  public EditPropertyAction update(ActionResponse action) {
    var payload = (ActionDetail.EditPropertyAction) action.getPayload();
    if (isBlank(payload.getName()) || isBlank(payload.getValue())) {
      throw new InvalidActionException();
    }
    this.setName(payload.getName());
    this.setValue(payload.getValue());
    return this;
  }

  @Override
  public ActionType getType() {
    return EDIT_PROPERTY;
  }
}
