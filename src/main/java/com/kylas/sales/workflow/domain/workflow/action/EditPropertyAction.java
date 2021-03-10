package com.kylas.sales.workflow.domain.workflow.action;

import static com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType.EDIT_PROPERTY;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kylas.sales.workflow.common.dto.ActionDetail;
import com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.ValueType;
import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class EditPropertyAction extends AbstractWorkflowAction implements com.kylas.sales.workflow.domain.workflow.action.WorkflowAction {

  private String name;
  @Convert(converter = ValueConverter.class)
  private Object value;
  @Enumerated(value = EnumType.STRING)
  private ValueType valueType;
  @JsonProperty(value = "isStandard")
  @Column(name = "is_standard")
  private boolean standard;

  private EditPropertyAction(String name, Object value, ValueType valueType, boolean standard) {
    this.name = name;
    this.value = value;
    this.valueType = valueType;
    this.standard = standard;

  }

  public static AbstractWorkflowAction createNew(ActionResponse actionResponse) {
    var payload = (ActionDetail.EditPropertyAction) actionResponse.getPayload();
    return new EditPropertyAction(payload.getName(), payload.getValue(), payload.getValueType(), payload.isStandard());
  }

  public static ActionResponse toActionResponse(EditPropertyAction action) {
    return new ActionResponse(action.getId(), EDIT_PROPERTY,
        new ActionDetail.EditPropertyAction(action.name, action.value, action.valueType, action.standard));
  }

  @Override
  public void setWorkflow(Workflow workflow) {
    super.setWorkflow(workflow);
  }

  @Override
  public EditPropertyAction update(ActionResponse action) {
    var payload = (ActionDetail.EditPropertyAction) action.getPayload();
    this.setName(payload.getName());
    this.setValue(payload.getValue());
    this.setValueType(payload.getValueType());
    this.setStandard(payload.isStandard());
    return this;
  }

  @Override
  public ActionType getType() {
    return EDIT_PROPERTY;
  }
}
