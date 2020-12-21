package com.kylas.sales.workflow.domain.workflow.action;

import static com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType.EDIT_PROPERTY;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.kylas.sales.workflow.common.dto.ActionDetail;
import com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.ValueType;
import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.domain.exception.InvalidActionException;
import com.kylas.sales.workflow.domain.exception.InvalidValueTypeException;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import java.util.Arrays;
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

  private EditPropertyAction(String name, Object value, ValueType valueType) {
    this.name = name;
    this.value = value;
    this.valueType = valueType;

  }

  public static AbstractWorkflowAction createNew(ActionResponse actionResponse) {
    var payload = (ActionDetail.EditPropertyAction) actionResponse.getPayload();
    if (isBlank(payload.getName()) || isNull(payload.getValue())) {
      throw new InvalidActionException();
    }
    if (isInValidValueType(payload.getName(), payload.getValueType())) {
      throw new InvalidValueTypeException();
    }
    return new EditPropertyAction(payload.getName(), payload.getValue(), payload.getValueType());
  }

  public static ActionResponse toActionResponse(EditPropertyAction action) {
    var editPropertyAction = new ActionDetail.EditPropertyAction(action.name, action.value, action.valueType);
    return new ActionResponse(action.getId(), EDIT_PROPERTY, editPropertyAction);
  }

  @Override
  public void setWorkflow(Workflow workflow) {
    super.setWorkflow(workflow);
  }

  @Override
  public EditPropertyAction update(ActionResponse action) {
    var payload = (ActionDetail.EditPropertyAction) action.getPayload();
    if (isBlank(payload.getName()) || isNull(payload.getValue())) {
      throw new InvalidActionException();
    }
    if (isInValidValueType(payload.getName(), payload.getValueType())) {
      throw new InvalidValueTypeException();
    }
    this.setName(payload.getName());
    this.setValue(payload.getValue());
    return this;
  }

  @Override
  public ActionType getType() {
    return EDIT_PROPERTY;
  }

  private static boolean isInValidValueType(String name, ValueType valueType) {
    if (isNull(valueType)) {
      return true;
    }
    if (Arrays.stream(ActionDetail.EditPropertyAction.FieldValueType.values()).anyMatch(value -> value.name().equalsIgnoreCase(name))) {
      return !ActionDetail.EditPropertyAction.FieldValueType.valueOf(name).getFieldValueType().equals(valueType);
    }
    return false;
  }
}
