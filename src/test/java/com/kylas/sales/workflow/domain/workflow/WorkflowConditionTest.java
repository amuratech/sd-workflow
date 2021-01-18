package com.kylas.sales.workflow.domain.workflow;

import static com.kylas.sales.workflow.api.request.Condition.TriggerType.NEW_VALUE;
import static com.kylas.sales.workflow.common.dto.condition.Operator.AND;
import static com.kylas.sales.workflow.common.dto.condition.Operator.BETWEEN;
import static com.kylas.sales.workflow.common.dto.condition.Operator.CONTAINS;
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
import static org.assertj.core.api.Assertions.assertThat;

import com.kylas.sales.workflow.common.dto.condition.WorkflowCondition.ConditionExpression;
import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import com.kylas.sales.workflow.domain.ConditionFacade;
import com.kylas.sales.workflow.domain.processor.lead.IdName;
import com.kylas.sales.workflow.domain.processor.lead.LeadDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(initializers = {TestDatabaseInitializer.class})
class WorkflowConditionTest {

  @Autowired
  private ConditionFacade conditionFacade;

  @Test
  public void givenEquals_onSameStrings_evaluatesTrue() {
    var expression = new ConditionExpression(EQUAL, "firstName", "Steve", NEW_VALUE);
    var entity = stubLeadDetail();
    entity.setFirstName("Steve");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
  }

  @Test
  public void givenEquals_onDifferentStrings_evaluatesFalse() {
    var expression = new ConditionExpression(EQUAL, "firstName", "Steve", NEW_VALUE);
    var entity = stubLeadDetail();
    entity.setFirstName("Tony");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isFalse();
  }

  @Test
  public void givenNotEquals_onDifferentStrings_evaluatesTrue() {
    var expression = new ConditionExpression(NOT_EQUAL, "firstName", "Steve", NEW_VALUE);
    var entity = stubLeadDetail();
    entity.setFirstName("Tony");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
  }

  @Test
  public void givenNotEquals_onSameStrings_evaluatesFalse() {
    var expression = new ConditionExpression(NOT_EQUAL, "firstName", "Steve", NEW_VALUE);
    var entity = stubLeadDetail();
    entity.setFirstName("Steve");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isFalse();
  }

  @Test
  public void givenEquals_onSameBooleans_evaluatesTrue() {
    var expression = new ConditionExpression(EQUAL, "dnd", "true", NEW_VALUE);
    var entity = stubLeadDetail();
    entity.setDnd(true);

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
  }

  @Test
  public void givenEquals_onDifferentBooleans_evaluatesFalse() {
    var expression = new ConditionExpression(EQUAL, "dnd", "false", NEW_VALUE);
    var entity = stubLeadDetail();
    entity.setDnd(true);

    assertThat(conditionFacade.satisfies(expression, entity))
        .isFalse();
  }

  @Test
  public void givenEquals_matchingStringsIgnoringCase_evaluatesTrue() {
    var expression = new ConditionExpression(EQUAL, "firstName", "Tony", NEW_VALUE);
    var entity = stubLeadDetail();
    entity.setFirstName("tony");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
  }

  @Test
  public void givenNotEquals_matchingStringsIgnoringCase_evaluatesFalse() {
    var expression = new ConditionExpression(NOT_EQUAL, "firstName", "Tony", NEW_VALUE);
    var entity = stubLeadDetail();
    entity.setFirstName("tony");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isFalse();
  }

  @Test
  public void givenIsNullOperator_onNullValue_evaluatesTrue() {
    var expression = new ConditionExpression(IS_NULL, "campaign", null, NEW_VALUE);
    var entity = stubLeadDetail();
    entity.setCampaign(null);

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
  }

  @Test
  public void givenIsNullOperator_onNonNullValue_evaluatesFalse() {

    var expression = new ConditionExpression(IS_NULL, "campaign", null, NEW_VALUE);
    var entity = stubLeadDetail();
    entity.setCampaign(new IdName(4000L, "SomeCampaign"));

    assertThat(conditionFacade.satisfies(expression, entity))
        .isFalse();
  }

  @Test
  public void givenContainsOperator_onMatchingValue_evaluatesTrue() {
    var expression = new ConditionExpression(CONTAINS, "firstName", "eve", NEW_VALUE);
    var entity = stubLeadDetail();
    entity.setFirstName("Steve");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
  }

  @Test
  public void givenContainsOperator_onNonMatchingValue_evaluatesFalse() {
    var expression = new ConditionExpression(CONTAINS, "campaign", "eve", NEW_VALUE);
    var entity = stubLeadDetail();
    entity.setFirstName("Tony");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isFalse();
  }

  @Test
  public void givenGreaterThanOperator_onGreaterValue_evaluatesTrue() {
    var expression = new ConditionExpression(GREATER, "companyAnnualRevenue", 900, NEW_VALUE);
    var entity = stubLeadDetail();
    entity.setCompanyAnnualRevenue(3000D);

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
  }

  @Test
  public void givenLessThanOperator_onLesserValue_evaluatesTrue() {
    var expression = new ConditionExpression(LESS, "companyAnnualRevenue", 900, NEW_VALUE);
    var entity = stubLeadDetail();
    entity.setCompanyAnnualRevenue(500D);

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
  }

  @Test
  public void givenInOperator_havingValue_evaluatesTrue() {
    var expression = new ConditionExpression(IN, "firstName", "val1,val2, val3", NEW_VALUE);
    var entity = stubLeadDetail();
    entity.setFirstName("val2");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
  }

  @Test
  public void givenInOperator_NotHavingValue_evaluatesFalse() {
    var expression = new ConditionExpression(IN, "firstName", "val1,val2, val3", NEW_VALUE);
    var entity = stubLeadDetail();
    entity.setFirstName("val6");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isFalse();
  }

  @Test
  public void givenNotInOperator_notHavingValue_evaluatesTrue() {
    var expression = new ConditionExpression(NOT_IN, "firstName", "val1,val2, val3", NEW_VALUE);
    var entity = stubLeadDetail();
    entity.setFirstName("val8");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
  }

  @Test
  public void givenNotInOperator_havingValue_evaluatesFalse() {
    var expression = new ConditionExpression(NOT_IN, "firstName", "val1,val2, val3", NEW_VALUE);
    var entity = stubLeadDetail();
    entity.setFirstName("val3");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isFalse();
  }

  @Test
  public void givenAndOperator_withBothExpressionsTrue_evaluatesTrue() {
    var operand1 = new ConditionExpression(EQUAL, "firstName", "Tony", NEW_VALUE);
    var operand2 = new ConditionExpression(EQUAL, "lastName", "Stark", NEW_VALUE);
    ConditionExpression expression = new ConditionExpression(operand1, operand2, AND);

    var entity = stubLeadDetail();
    entity.setFirstName("Tony");
    entity.setLastName("Stark");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
  }

  @Test
  public void givenAndOperator_onlyOneEvaluatingTrue_evaluatesFalse() {
    var operand1 = new ConditionExpression(EQUAL, "firstName", "Tony", NEW_VALUE);
    var operand2 = new ConditionExpression(EQUAL, "lastName", "Stark", NEW_VALUE);
    ConditionExpression expression = new ConditionExpression(operand1, operand2, AND);

    var entity = stubLeadDetail();
    entity.setFirstName("Pepper");
    entity.setLastName("Stark");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isFalse();
  }

  @Test
  public void givenOrOperator_onlyOneEvaluatingTrue_evaluatesTrue() {
    var operand1 = new ConditionExpression(EQUAL, "firstName", "Tony", NEW_VALUE);
    var operand2 = new ConditionExpression(EQUAL, "lastName", "Stark", NEW_VALUE);
    ConditionExpression expression = new ConditionExpression(operand1, operand2, OR);

    var entity = stubLeadDetail();
    entity.setFirstName("Pepper");
    entity.setLastName("Stark");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
  }

  @Test
  public void givenNotEquals_onDifferentNumbers_evaluatesTrue() {
    var expression = new ConditionExpression(NOT_EQUAL, "companyAnnualRevenue", 500.50, NEW_VALUE);
    var entity = stubLeadDetail();
    entity.setCompanyAnnualRevenue(500D);

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
  }

  @Nested
  @DisplayName("Condition tests for IdName values")
  @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
  @AutoConfigureTestDatabase(replace = Replace.NONE)
  @ContextConfiguration(initializers = {TestDatabaseInitializer.class})
  public class IdNameConditionTest {

    @Test
    public void givenEquals_onSameIds_evaluatesTrue() {
      Object pipeline = new IdName(242L, "Routing pipeline");
      ConditionExpression expression = new ConditionExpression(EQUAL, "pipeline", pipeline, NEW_VALUE);
      var entity = stubLeadDetail();
      entity.setPipeline(new IdName(242L, "Routing pipeline"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
    }

    @Test
    public void givenEquals_onDifferentIds_evaluatesFalse() {
      Object pipeline = new IdName(242L, "Routing pipeline");
      var expression = new ConditionExpression(EQUAL, "pipeline", pipeline, NEW_VALUE);
      var entity = stubLeadDetail();
      entity.setPipeline(new IdName(900L, "Routing pipeline"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
    }

    @Test
    public void givenNotEquals_onSameIds_evaluatesFalse() {
      Object pipeline = new IdName(242L, "Routing pipeline");
      var expression = new ConditionExpression(NOT_EQUAL, "pipeline", pipeline, NEW_VALUE);
      var entity = stubLeadDetail();
      entity.setPipeline(new IdName(242L, "Routing pipeline"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
    }

    @Test
    public void givenNotEquals_onDifferentIds_evaluatesTrue() {
      Object pipeline = new IdName(242L, "Routing pipeline");
      var expression = new ConditionExpression(NOT_EQUAL, "pipeline", pipeline, NEW_VALUE);
      var entity = stubLeadDetail();
      entity.setPipeline(new IdName(200L, "Routing pipeline"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
    }

    @Test
    public void givenIsNullOperator_withNullValue_evaluatesTrue() {
      var expression = new ConditionExpression(IS_NULL, "pipeline", null, NEW_VALUE);
      var entity = stubLeadDetail();
      entity.setPipeline(null);

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
    }

    @Test
    public void givenIsNotNullOperator_withSomeValue_evaluatesTrue() {
      var expression = new ConditionExpression(IS_NOT_NULL, "pipeline", null, NEW_VALUE);
      var entity = stubLeadDetail();
      entity.setPipeline(new IdName(200L, "Routing pipeline"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
    }

    @Test
    public void givenBetweenOperator_havingValueInBetween_evaluatesTrue() {
      var expression = new ConditionExpression(BETWEEN, "requirementBudget", "[12,20]", NEW_VALUE);
      var entity = stubLeadDetail();
      entity.setRequirementBudget(15D);

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
    }

    @Test
    public void givenNotBetweenOperator_havingValueOutsideRange_evaluatesTrue() {
      var expression = new ConditionExpression(NOT_BETWEEN, "requirementBudget", "[12,20]", NEW_VALUE);
      var entity = stubLeadDetail();
      entity.setRequirementBudget(100D);

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
    }

    @Test
    public void givenEquals_onSameIdsSource_evaluatesTrue() {
      Object source = 100;
      var expression = new ConditionExpression(EQUAL, "source", source, NEW_VALUE);
      var entity = stubLeadDetail();
      entity.setSource(new IdName(100L, "Google"));

      assertThat(conditionFacade.satisfies(expression, entity)).isTrue();
    }

    @Test
    public void givenNotEquals_onDifferentIdsCampaign_evaluatesTrue() {
      Object campaign = 100;
      var expression = new ConditionExpression(NOT_EQUAL, "campaign", campaign, NEW_VALUE);
      var entity = stubLeadDetail();
      entity.setCampaign(new IdName(101L, "Organic"));

      assertThat(conditionFacade.satisfies(expression, entity)).isTrue();
    }

    @Test
    public void givenEquals_onDifferentIdsSalutation_evaluatesFalse() {
      Object salutation = 20;
      var expression = new ConditionExpression(EQUAL, "salutation", salutation, NEW_VALUE);
      var entity = stubLeadDetail();
      entity.setSalutation(new IdName(11L, "Miss"));

      assertThat(conditionFacade.satisfies(expression, entity)).isFalse();
    }

    @Test
    public void givenEquals_onSameUserId_evaluatesTrue() {
      Object createdBy = new IdName(200L, "Steve Roger");
      ConditionExpression expression = new ConditionExpression(EQUAL, "createdBy", createdBy, NEW_VALUE);
      var entity = stubLeadDetail();
      entity.setCreatedBy(new IdName(200L, "Steve Roger"));

      assertThat(conditionFacade.satisfies(expression, entity)).isTrue();
    }

    @Test
    public void givenEquals_onDifferentUserId_evaluatesFalse() {
      Object updatedBy = new IdName(200L, "Steve Roger");
      ConditionExpression expression = new ConditionExpression(EQUAL, "updatedBy", updatedBy, NEW_VALUE);
      var entity = stubLeadDetail();
      entity.setUpdatedBy(new IdName(201L, "Tony Stark"));

      assertThat(conditionFacade.satisfies(expression, entity)).isFalse();
    }

    @Test
    public void givenNotEquals_onDifferentUserId_evaluatesTrue() {
      Object convertedBy = new IdName(200L, "Steve Roger");
      ConditionExpression expression = new ConditionExpression(NOT_EQUAL, "convertedBy", convertedBy, NEW_VALUE);
      var entity = stubLeadDetail();
      entity.setConvertedBy(new IdName(201L, "Tony Stark"));

      assertThat(conditionFacade.satisfies(expression, entity)).isTrue();
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