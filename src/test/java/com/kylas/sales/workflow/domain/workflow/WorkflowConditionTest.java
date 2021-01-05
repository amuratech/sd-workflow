package com.kylas.sales.workflow.domain.workflow;

import static com.kylas.sales.workflow.common.dto.condition.Operator.AND;
import static com.kylas.sales.workflow.common.dto.condition.Operator.BETWEEN;
import static com.kylas.sales.workflow.common.dto.condition.Operator.EQUAL;
import static com.kylas.sales.workflow.common.dto.condition.Operator.GREATER;
import static com.kylas.sales.workflow.common.dto.condition.Operator.IN;
import static com.kylas.sales.workflow.common.dto.condition.Operator.IS_NOT_NULL;
import static com.kylas.sales.workflow.common.dto.condition.Operator.IS_NULL;
import static com.kylas.sales.workflow.common.dto.condition.Operator.LESS;
import static com.kylas.sales.workflow.common.dto.condition.Operator.NOT_BETWEEN;
import static com.kylas.sales.workflow.common.dto.condition.Operator.NOT_EQUAL;
import static com.kylas.sales.workflow.common.dto.condition.Operator.NOT_IN;
import static com.kylas.sales.workflow.common.dto.condition.Operator.OR;
import static com.kylas.sales.workflow.common.dto.condition.WorkflowCondition.TriggerType.NEW_VALUE;
import static org.assertj.core.api.Assertions.assertThat;

import com.kylas.sales.workflow.common.dto.condition.Operator;
import com.kylas.sales.workflow.common.dto.condition.WorkflowCondition.ConditionExpression;
import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import com.kylas.sales.workflow.domain.processor.lead.IdName;
import com.kylas.sales.workflow.domain.processor.lead.LeadDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;

class WorkflowConditionTest {

  @Test
  public void givenConditionForAllLeads_alwaysEvaluatesTrue() {
    var condition = new WorkflowCondition();
    condition.setType(ConditionType.FOR_ALL);
    condition.setExpression(null);
    assertThat(condition.isSatisfiedBy(stubLeadDetail())).isTrue();
  }

  @Test
  public void givenEquals_onSameStrings_evaluatesTrue() {
    var condition = new WorkflowCondition();
    condition.setType(ConditionType.CONDITION_BASED);
    condition.setExpression(new ConditionExpression(EQUAL, "firstName", "Steve", NEW_VALUE));
    var entity = stubLeadDetail();
    entity.setFirstName("Steve");
    assertThat(condition.isSatisfiedBy(entity)).isTrue();
  }

  @Test
  public void givenEquals_onDifferentStrings_evaluatesFalse() {
    var condition = new WorkflowCondition();
    condition.setType(ConditionType.CONDITION_BASED);
    condition.setExpression(new ConditionExpression(EQUAL, "firstName", "Tony", NEW_VALUE));
    var entity = stubLeadDetail();
    entity.setFirstName("Steve");
    assertThat(condition.isSatisfiedBy(entity)).isFalse();
  }

  @Test
  public void givenNotEquals_onDifferentStrings_evaluatesTrue() {
    var condition = new WorkflowCondition();
    condition.setType(ConditionType.CONDITION_BASED);
    condition.setExpression(new ConditionExpression(Operator.NOT_EQUAL, "firstName", "Tony", NEW_VALUE));
    var entity = stubLeadDetail();
    entity.setFirstName("Steve");
    assertThat(condition.isSatisfiedBy(entity)).isTrue();
  }

  @Test
  public void givenNotEquals_onSameStrings_evaluatesFalse() {
    var condition = new WorkflowCondition();
    condition.setType(ConditionType.CONDITION_BASED);
    condition.setExpression(new ConditionExpression(Operator.NOT_EQUAL, "firstName", "Tony", NEW_VALUE));
    var entity = stubLeadDetail();
    entity.setFirstName("Tony");
    assertThat(condition.isSatisfiedBy(entity)).isFalse();
  }

  @Test
  public void givenEquals_onSameBooleans_evaluatesTrue() {
    var condition = new WorkflowCondition();
    condition.setType(ConditionType.CONDITION_BASED);
    condition.setExpression(new ConditionExpression(EQUAL, "dnd", "true", NEW_VALUE));
    var entity = stubLeadDetail();
    entity.setDnd(true);
    assertThat(condition.isSatisfiedBy(entity)).isTrue();
  }

  @Test
  public void givenEquals_onDifferentBooleans_evaluatesFalse() {
    var condition = new WorkflowCondition();
    condition.setType(ConditionType.CONDITION_BASED);
    condition.setExpression(new ConditionExpression(EQUAL, "dnd", "false", NEW_VALUE));
    var entity = stubLeadDetail();
    entity.setDnd(true);
    assertThat(condition.isSatisfiedBy(entity)).isFalse();
  }

  @Test
  public void givenIsNullOperator_onNullValue_evaluatesTrue() {
    var condition = new WorkflowCondition();
    condition.setType(ConditionType.CONDITION_BASED);
    condition.setExpression(new ConditionExpression(Operator.IS_NULL, "campaign", null, NEW_VALUE));
    var entity = stubLeadDetail();
    entity.setCampaign(null);
    assertThat(condition.isSatisfiedBy(entity)).isTrue();
  }

  @Test
  public void givenIsNullOperator_onNonNullValue_evaluatesFalse() {
    var condition = new WorkflowCondition();
    condition.setType(ConditionType.CONDITION_BASED);
    condition.setExpression(new ConditionExpression(Operator.IS_NULL, "campaign", null, NEW_VALUE));
    var entity = stubLeadDetail();
    entity.setCampaign(new IdName(4000L, "SomeCampaign"));
    assertThat(condition.isSatisfiedBy(entity)).isFalse();
  }

  @Test
  public void givenContainsOperator_onMatchingValue_evaluatesTrue() {
    var condition = new WorkflowCondition();
    condition.setType(ConditionType.CONDITION_BASED);
    condition.setExpression(new ConditionExpression(Operator.CONTAINS, "firstName", "eve", NEW_VALUE));
    var entity = stubLeadDetail();
    entity.setFirstName("Steve");
    assertThat(condition.isSatisfiedBy(entity)).isTrue();
  }

  @Test
  public void givenContainsOperator_onNonMatchingValue_evaluatesFalse() {
    var condition = new WorkflowCondition();
    condition.setType(ConditionType.CONDITION_BASED);
    condition.setExpression(new ConditionExpression(Operator.CONTAINS, "firstName", "eve", NEW_VALUE));
    var entity = stubLeadDetail();
    entity.setFirstName("Tony");
    assertThat(condition.isSatisfiedBy(entity)).isFalse();
  }

  @Test
  public void givenGreaterThanOperator_onGreaterValue_evaluatesTrue() {
    var condition = new WorkflowCondition();
    condition.setType(ConditionType.CONDITION_BASED);
    condition.setExpression(new ConditionExpression(GREATER, "companyAnnualRevenue", 900, NEW_VALUE));
    var entity = stubLeadDetail();
    entity.setCompanyAnnualRevenue(3000D);
    assertThat(condition.isSatisfiedBy(entity)).isTrue();
  }

  @Test
  public void givenGreaterOrEqualsOperator_onGreaterValue_evaluatesTrue() {
    var condition = new WorkflowCondition();
    condition.setType(ConditionType.CONDITION_BASED);
    condition.setExpression(new ConditionExpression(GREATER, "companyAnnualRevenue", 900, NEW_VALUE));
    var entity = stubLeadDetail();
    entity.setCompanyAnnualRevenue(3000D);
    assertThat(condition.isSatisfiedBy(entity)).isTrue();
  }

  @Test
  public void givenLessThanOperator_onLesserValue_evaluatesTrue() {
    var condition = new WorkflowCondition();
    condition.setType(ConditionType.CONDITION_BASED);
    condition.setExpression(new ConditionExpression(LESS, "companyAnnualRevenue", 900, NEW_VALUE));
    var entity = stubLeadDetail();
    entity.setCompanyAnnualRevenue(500D);
    assertThat(condition.isSatisfiedBy(entity)).isTrue();
  }

  @Test
  public void givenInOperator_havingValue_evaluatesTrue() {
    var condition = new WorkflowCondition();
    condition.setType(ConditionType.CONDITION_BASED);
    condition.setExpression(new ConditionExpression(IN, "firstName", "val1, val2, val3", NEW_VALUE));
    var entity = stubLeadDetail();
    entity.setFirstName("val2");
    assertThat(condition.isSatisfiedBy(entity)).isTrue();
  }

  @Test
  public void givenInOperator_NotHavingValue_evaluatesFalse() {
    var condition = new WorkflowCondition();
    condition.setType(ConditionType.CONDITION_BASED);
    condition.setExpression(new ConditionExpression(IN, "firstName", "val1, val2, val3", NEW_VALUE));
    var entity = stubLeadDetail();
    entity.setFirstName("val4");
    assertThat(condition.isSatisfiedBy(entity)).isFalse();
  }

  @Test
  public void givenNotInOperator_notHavingValue_evaluatesTrue() {
    var condition = new WorkflowCondition();
    condition.setType(ConditionType.CONDITION_BASED);
    condition.setExpression(new ConditionExpression(NOT_IN, "firstName", "val1, val2, val3", NEW_VALUE));
    var entity = stubLeadDetail();
    entity.setFirstName("val4");
    assertThat(condition.isSatisfiedBy(entity)).isTrue();
  }

  @Test
  public void givenNotInOperator_havingValue_evaluatesFalse() {
    var condition = new WorkflowCondition();
    condition.setType(ConditionType.CONDITION_BASED);
    condition.setExpression(new ConditionExpression(NOT_IN, "firstName", "val1, val2, val3", NEW_VALUE));
    var entity = stubLeadDetail();
    entity.setFirstName("val3");
    assertThat(condition.isSatisfiedBy(entity)).isFalse();
  }

  @Test
  public void givenAndOperator_withBothExpressionsTrue_evaluatesTrue() {
    var condition = new WorkflowCondition();
    condition.setType(ConditionType.CONDITION_BASED);
    var operand1 = new ConditionExpression(EQUAL, "firstName", "Tony", NEW_VALUE);
    var operand2 = new ConditionExpression(EQUAL, "lastName", "Stark", NEW_VALUE);
    condition.setExpression(new ConditionExpression(operand1, operand2, AND, NEW_VALUE));

    var entity = stubLeadDetail();
    entity.setFirstName("Tony");
    entity.setLastName("Stark");

    assertThat(condition.isSatisfiedBy(entity)).isTrue();
  }

  @Test
  public void givenAndOperator_onlyOneEvaluatingTrue_evaluatesFalse() {
    var condition = new WorkflowCondition();
    condition.setType(ConditionType.CONDITION_BASED);
    var operand1 = new ConditionExpression(EQUAL, "firstName", "Tony", NEW_VALUE);
    var operand2 = new ConditionExpression(EQUAL, "lastName", "Stark", NEW_VALUE);
    condition.setExpression(new ConditionExpression(operand1, operand2, AND, NEW_VALUE));

    var entity = stubLeadDetail();
    entity.setFirstName("Pepper");
    entity.setLastName("Stark");

    assertThat(condition.isSatisfiedBy(entity)).isFalse();
  }

  @Test
  public void givenOrOperator_onlyOneEvaluatingTrue_evaluatesTrue() {
    var condition = new WorkflowCondition();
    condition.setType(ConditionType.CONDITION_BASED);
    var operand1 = new ConditionExpression(EQUAL, "firstName", "Tony", NEW_VALUE);
    var operand2 = new ConditionExpression(EQUAL, "lastName", "Stark", NEW_VALUE);
    condition.setExpression(new ConditionExpression(operand1, operand2, OR, NEW_VALUE));

    var entity = stubLeadDetail();
    entity.setFirstName("Pepper");
    entity.setLastName("Stark");

    assertThat(condition.isSatisfiedBy(entity)).isTrue();
  }

  @Nested
  @DisplayName("Condition tests for IdName values")
  @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
  @AutoConfigureTestDatabase(replace = Replace.NONE)
  @ContextConfiguration(initializers = {TestDatabaseInitializer.class})
  public class IdNameConditionTest {

    @Test
    public void givenEquals_onSameIds_evaluatesTrue() {
      var condition = new WorkflowCondition();
      condition.setType(ConditionType.CONDITION_BASED);
      Object pipeline = "{\"id\":242,\"name\":\"Routing pipeline\"}";
      condition.setExpression(new ConditionExpression(EQUAL, "pipeline", pipeline, NEW_VALUE));
      var entity = stubLeadDetail();
      entity.setPipeline(new IdName(242L, "Routing pipeline"));

      assertThat(condition.isSatisfiedBy(entity)).isTrue();
    }

    @Test
    public void givenEquals_onDifferentIds_evaluatesFalse() {
      var condition = new WorkflowCondition();
      condition.setType(ConditionType.CONDITION_BASED);
      Object pipeline = "{\"id\":242,\"name\":\"Routing pipeline\"}";
      condition.setExpression(new ConditionExpression(EQUAL, "pipeline", pipeline, NEW_VALUE));
      var entity = stubLeadDetail();
      entity.setPipeline(new IdName(243L, "Routing pipeline"));

      assertThat(condition.isSatisfiedBy(entity)).isFalse();
    }

    @Test
    public void givenNotEquals_onSameIds_evaluatesFalse() {
      var condition = new WorkflowCondition();
      condition.setType(ConditionType.CONDITION_BASED);
      Object pipeline = "{\"id\":242,\"name\":\"Routing pipeline\"}";
      condition.setExpression(new ConditionExpression(NOT_EQUAL, "pipeline", pipeline, NEW_VALUE));
      var entity = stubLeadDetail();
      entity.setPipeline(new IdName(242L, "Routing pipeline"));

      assertThat(condition.isSatisfiedBy(entity)).isFalse();
    }

    @Test
    public void givenNotEquals_onDifferentIds_evaluatesTrue() {
      var condition = new WorkflowCondition();
      condition.setType(ConditionType.CONDITION_BASED);
      Object pipeline = "{\"id\":242,\"name\":\"Routing pipeline\"}";
      condition.setExpression(new ConditionExpression(NOT_EQUAL, "pipeline", pipeline, NEW_VALUE));
      var entity = stubLeadDetail();
      entity.setPipeline(new IdName(243L, "Routing pipeline"));

      assertThat(condition.isSatisfiedBy(entity)).isTrue();
    }

    @Test
    public void givenIsNullOperator_withNullValue_evaluatesTrue() {
      var condition = new WorkflowCondition();
      condition.setType(ConditionType.CONDITION_BASED);
      condition.setExpression(new ConditionExpression(IS_NULL, "pipeline", null, NEW_VALUE));
      var entity = stubLeadDetail();
      entity.setPipeline(null);

      assertThat(condition.isSatisfiedBy(entity)).isTrue();
    }

    @Test
    public void givenIsNotNullOperator_withSomeValue_evaluatesTrue() {
      var condition = new WorkflowCondition();
      condition.setType(ConditionType.CONDITION_BASED);
      condition.setExpression(new ConditionExpression(IS_NOT_NULL, "pipeline", null, NEW_VALUE));
      var entity = stubLeadDetail();
      entity.setPipeline(new IdName(242L, "Some-Name"));

      assertThat(condition.isSatisfiedBy(entity)).isTrue();
    }

    @Test
    public void givenBetweenOperator_havingValueInBetween_evaluatesTrue() {
      var condition = new WorkflowCondition();
      condition.setType(ConditionType.CONDITION_BASED);
      condition.setExpression(new ConditionExpression(BETWEEN, "requirementBudget", "[12,20]", NEW_VALUE));
      var entity = stubLeadDetail();
      entity.setRequirementBudget(15D);
      assertThat(condition.isSatisfiedBy(entity)).isTrue();
    }

    @Test
    public void givenNotBetweenOperator_havingValueOutsideRange_evaluatesTrue() {
      var condition = new WorkflowCondition();
      condition.setType(ConditionType.CONDITION_BASED);
      condition.setExpression(new ConditionExpression(NOT_BETWEEN, "requirementBudget", "[12,20]", NEW_VALUE));
      var entity = stubLeadDetail();
      entity.setRequirementBudget(25D);
      assertThat(condition.isSatisfiedBy(entity)).isTrue();
    }
  }

  private LeadDetail stubLeadDetail() {
    var leadDetail = new LeadDetail();
    leadDetail.setId(2000L);
    leadDetail.setFirstName("Steve");
    leadDetail.setLastName("Rogers");
    leadDetail.setName("Steve Rogers");
    leadDetail.setCity("Pune");
    return leadDetail;
  }

}