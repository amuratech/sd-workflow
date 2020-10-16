package com.kylas.sales.workflow.domain.workflow.action;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.kylas.sales.workflow.api.request.ActionRequest;
import com.kylas.sales.workflow.domain.exception.InvalidActionException;
import com.kylas.sales.workflow.domain.processor.Actionable;
import com.kylas.sales.workflow.domain.processor.exception.WorkflowExecutionException;
import com.kylas.sales.workflow.domain.processor.lead.Lead;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.error.ErrorCode;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
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
public class EditPropertyAction extends AbstractWorkflowAction implements WorkflowAction {

  private String name;
  private String value;

  private EditPropertyAction(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public static Set<AbstractWorkflowAction> createNew(Set<ActionRequest> actions) {
    return actions.stream()
        .filter(actionRequest -> ActionType.EDIT_PROPERTY.equals(actionRequest.getType()))
        .map(actionRequest -> {
          if (isBlank(actionRequest.getPayload().getName()) || isBlank(actionRequest.getPayload().getValue())) {
            throw new InvalidActionException();
          }
          return new EditPropertyAction(actionRequest.getPayload().getName(), actionRequest.getPayload().getValue());
        })
        .collect(Collectors.toCollection(HashSet::new));
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
}
