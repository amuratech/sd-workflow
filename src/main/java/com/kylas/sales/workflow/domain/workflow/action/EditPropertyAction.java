package com.kylas.sales.workflow.domain.workflow.action;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.kylas.sales.workflow.api.request.ActionRequest;
import com.kylas.sales.workflow.domain.exception.InvalidActionException;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class EditPropertyAction extends AbstractWorkflowAction implements WorkflowAction {

  private String name;
  private String value;

  private EditPropertyAction(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public static Set<WorkflowAction> createNew(Set<ActionRequest> actions) {
    return actions.stream()
        .filter(actionRequest -> ActionType.EDIT_PROPERTY.equals(actionRequest.getType()))
        .map(actionRequest -> {
          if(isBlank(actionRequest.getPayload().getName()) || isBlank(actionRequest.getPayload().getValue())){
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
}
