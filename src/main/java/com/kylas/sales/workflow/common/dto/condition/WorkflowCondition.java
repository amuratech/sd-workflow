package com.kylas.sales.workflow.common.dto.condition;

import static com.kylas.sales.workflow.common.dto.condition.Operator.AND;
import static com.kylas.sales.workflow.common.dto.condition.Operator.OR;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kylas.sales.workflow.api.request.Condition;
import com.kylas.sales.workflow.api.request.Condition.TriggerType;
import com.kylas.sales.workflow.domain.workflow.ConditionType;
import java.io.Serializable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Slf4j
public class WorkflowCondition {

  private final ConditionType conditionType;
  private final ConditionExpression expression;

  @JsonCreator
  public WorkflowCondition(
      @JsonProperty("conditionType") ConditionType conditionType,
      @JsonProperty("expression") ConditionExpression expression) {
    this.conditionType = conditionType;
    this.expression = expression;
  }

  @Getter
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ConditionExpression implements Serializable {

    private final ConditionExpression operand1;
    private final ConditionExpression operand2;
    private final Operator operator;
    private final String name;
    private final Object value;
    private final TriggerType triggerOn;

    @JsonCreator
    public ConditionExpression(
        @JsonProperty("operand1") ConditionExpression operand1,
        @JsonProperty("operand2") ConditionExpression operand2,
        @JsonProperty("operator") String operator,
        @JsonProperty("name") String name,
        @JsonProperty("value") Object value,
        @JsonProperty("triggerOn") String triggerOn) {
      this.operand1 = operand1;
      this.operand2 = operand2;
      this.operator = Operator.getByName(operator);
      this.name = name;
      this.value = value;
      if (TriggerType.IS_CHANGED.name().equals(triggerOn)) {
        this.triggerOn = Condition.TriggerType.valueOf(triggerOn);
      } else {
        this.triggerOn = (this.operator.equals(AND) || this.operator.equals(OR)) ? null : Condition.TriggerType.valueOf(triggerOn);
      }
    }

    public ConditionExpression(Operator operator, String name, Object value, TriggerType triggerType) {
      this.operator = operator;
      this.name = name;
      this.value = value;
      this.triggerOn = triggerType;
      operand1 = null;
      operand2 = null;
    }

    public ConditionExpression(ConditionExpression operand1, ConditionExpression operand2, Operator operator) {
      this.operand1 = operand1;
      this.operand2 = operand2;
      this.operator = operator;
      triggerOn = null;
      value = null;
      name = null;
    }

    public boolean isTerminalOperator(Operator operator) {
      return operator.equals(this.operator) && this.operand1 == null && this.operand2 == null;
    }
  }

}
