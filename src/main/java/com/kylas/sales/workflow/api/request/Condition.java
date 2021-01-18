package com.kylas.sales.workflow.api.request;

import static com.kylas.sales.workflow.common.dto.condition.Operator.AND;
import static com.kylas.sales.workflow.common.dto.condition.Operator.OR;
import static com.kylas.sales.workflow.common.dto.condition.Operator.getByName;
import static java.util.Objects.isNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kylas.sales.workflow.common.dto.condition.Operator;
import com.kylas.sales.workflow.domain.workflow.ConditionType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@JsonInclude(Include.NON_NULL)
public class Condition {

  private final ConditionType conditionType;
  private final List<ExpressionElement> conditions;

  @JsonCreator
  public Condition(
      @JsonProperty("conditionType") String conditionType,
      @JsonProperty("conditions") List<ExpressionElement> conditions) {
    this.conditionType = ConditionType.valueOf(conditionType);
    this.conditions = conditions;
  }

  @Getter
  @JsonInclude(Include.NON_NULL)
  public static class ExpressionElement {

    private final Operator operator;
    private final String name;
    private final Object value;
    private final TriggerType triggerOn;

    @JsonCreator
    public ExpressionElement(
        @JsonProperty("operator") String operator,
        @JsonProperty("name") String name,
        @JsonProperty("value") Object value,
        @JsonProperty("triggerOn") String triggerOn) {
      this.operator = getByName(operator);
      this.name = name;
      this.value = value;
      this.triggerOn = isNull(triggerOn) ? null : TriggerType.valueOf(triggerOn);
    }

    public boolean hasBinaryOperator() {
      return operator.equals(AND) || operator.equals(OR);
    }
  }

  @Getter
  @AllArgsConstructor
  public enum TriggerType {
    NEW_VALUE
  }
}
