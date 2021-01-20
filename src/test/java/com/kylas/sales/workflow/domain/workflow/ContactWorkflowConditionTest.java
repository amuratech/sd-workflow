package com.kylas.sales.workflow.domain.workflow;

import static com.kylas.sales.workflow.api.request.Condition.TriggerType.NEW_VALUE;
import static com.kylas.sales.workflow.common.dto.condition.Operator.AND;
import static com.kylas.sales.workflow.common.dto.condition.Operator.CONTAINS;
import static com.kylas.sales.workflow.common.dto.condition.Operator.EQUAL;
import static com.kylas.sales.workflow.common.dto.condition.Operator.GREATER;
import static com.kylas.sales.workflow.common.dto.condition.Operator.IN;
import static com.kylas.sales.workflow.common.dto.condition.Operator.IS_NULL;
import static com.kylas.sales.workflow.common.dto.condition.Operator.LESS;
import static com.kylas.sales.workflow.common.dto.condition.Operator.NOT_EQUAL;
import static com.kylas.sales.workflow.common.dto.condition.Operator.NOT_IN;
import static com.kylas.sales.workflow.common.dto.condition.Operator.OR;
import static org.assertj.core.api.Assertions.assertThat;

import com.kylas.sales.workflow.common.dto.condition.WorkflowCondition.ConditionExpression;
import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import com.kylas.sales.workflow.domain.ConditionFacade;
import com.kylas.sales.workflow.domain.processor.contact.ContactDetail;
import com.kylas.sales.workflow.domain.processor.lead.IdName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(initializers = {TestDatabaseInitializer.class})
class ContactWorkflowConditionTest {

  @Autowired
  private ConditionFacade conditionFacade;

  @Test
  public void givenEquals_onSameStrings_evaluatesTrue() {
    var expression = new ConditionExpression(EQUAL, "firstName", "Yash", NEW_VALUE);
    var entity = stubContactDetail();
    entity.setFirstName("Yash");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
  }

  @Test
  public void givenEquals_onDifferentStrings_evaluatesFalse() {
    var expression = new ConditionExpression(EQUAL, "firstName", "Yash", NEW_VALUE);
    var entity = stubContactDetail();
    entity.setFirstName("Om");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isFalse();
  }

  @Test
  public void givenNotEquals_onDifferentStrings_evaluatesTrue() {
    var expression = new ConditionExpression(NOT_EQUAL, "firstName", "Yash", NEW_VALUE);
    var entity = stubContactDetail();
    entity.setFirstName("Om");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
  }

  @Test
  public void givenNotEquals_onSameStrings_evaluatesFalse() {
    var expression = new ConditionExpression(NOT_EQUAL, "firstName", "Yash", NEW_VALUE);
    var entity = stubContactDetail();
    entity.setFirstName("Yash");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isFalse();
  }

  @Test
  public void givenEquals_onSameBooleans_evaluatesTrue() {
    var expression = new ConditionExpression(EQUAL, "stakeholder", "true", NEW_VALUE);
    var entity = stubContactDetail();
    entity.setStakeholder(true);

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
  }

  @Test
  public void givenEquals_onDifferentBooleans_evaluatesFalse() {
    var expression = new ConditionExpression(EQUAL, "stakeholder", "false", NEW_VALUE);
    var entity = stubContactDetail();
    entity.setStakeholder(true);

    assertThat(conditionFacade.satisfies(expression, entity))
        .isFalse();
  }

  @Test
  public void givenEquals_matchingStringsIgnoringCase_evaluatesTrue() {
    var expression = new ConditionExpression(EQUAL, "firstName", "Yash", NEW_VALUE);
    var entity = stubContactDetail();
    entity.setFirstName("yash");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
  }

  @Test
  public void givenNotEquals_matchingStringsIgnoringCase_evaluatesFalse() {
    var expression = new ConditionExpression(NOT_EQUAL, "firstName", "Yash", NEW_VALUE);
    var entity = stubContactDetail();
    entity.setFirstName("yash");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isFalse();
  }

  @Test
  public void givenIsNullOperator_onNullValue_evaluatesTrue() {
    var expression = new ConditionExpression(IS_NULL, "createdBy", null, NEW_VALUE);
    var entity = stubContactDetail();
    entity.setCreatedBy(null);

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
  }

  @Test
  public void givenIsNullOperator_onNonNullValue_evaluatesFalse() {

    var expression = new ConditionExpression(IS_NULL, "createdBy", null, NEW_VALUE);
    var entity = stubContactDetail();
    entity.setCreatedBy(new IdName(4000L, "John"));

    assertThat(conditionFacade.satisfies(expression, entity))
        .isFalse();
  }

  @Test
  public void givenContainsOperator_onMatchingValue_evaluatesTrue() {
    var expression = new ConditionExpression(CONTAINS, "firstName", "ash", NEW_VALUE);
    var entity = stubContactDetail();
    entity.setFirstName("Yash");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
  }

  @Test
  public void givenContainsOperator_onNonMatchingValue_evaluatesFalse() {
    var expression = new ConditionExpression(CONTAINS, "firstName", "ash", NEW_VALUE);
    var entity = stubContactDetail();
    entity.setFirstName("Om");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isFalse();
  }

  @Test
  public void givenGreaterThanOperator_onGreaterValue_evaluatesTrue() {
    var expression = new ConditionExpression(GREATER, "id", 900, NEW_VALUE);
    var entity = stubContactDetail();
    entity.setId(3000L);

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
  }

  @Test
  public void givenLessThanOperator_onLesserValue_evaluatesTrue() {
    var expression = new ConditionExpression(LESS, "id", 900, NEW_VALUE);
    var entity = stubContactDetail();
    entity.setId(500L);

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
  }

  @Test
  public void givenInOperator_havingValue_evaluatesTrue() {
    var expression = new ConditionExpression(IN, "firstName", "val1, val2, val3", NEW_VALUE);
    var entity = stubContactDetail();
    entity.setFirstName("val2");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
  }

  @Test
  public void givenInOperator_NotHavingValue_evaluatesFalse() {
    var expression = new ConditionExpression(IN, "firstName", "val1, val2, val3", NEW_VALUE);
    var entity = stubContactDetail();
    entity.setFirstName("val6");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isFalse();
  }

  @Test
  public void givenNotInOperator_notHavingValue_evaluatesTrue() {
    var expression = new ConditionExpression(NOT_IN, "firstName", "val1, val2, val3", NEW_VALUE);
    var entity = stubContactDetail();
    entity.setFirstName("val8");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
  }

  @Test
  public void givenNotInOperator_havingValue_evaluatesFalse() {
    var expression = new ConditionExpression(NOT_IN, "firstName", "val1, val2, val3", NEW_VALUE);
    var entity = stubContactDetail();
    entity.setFirstName("val3");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isFalse();
  }

  @Test
  public void givenAndOperator_withBothExpressionsTrue_evaluatesTrue() {
    var operand1 = new ConditionExpression(EQUAL, "firstName", "Yash", NEW_VALUE);
    var operand2 = new ConditionExpression(EQUAL, "lastName", "Kshatriya", NEW_VALUE);
    ConditionExpression expression = new ConditionExpression(operand1, operand2, AND);

    var entity = stubContactDetail();
    entity.setFirstName("Yash");
    entity.setLastName("Kshatriya");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
  }

  @Test
  public void givenAndOperator_onlyOneEvaluatingTrue_evaluatesFalse() {
    var operand1 = new ConditionExpression(EQUAL, "firstName", "Yash", NEW_VALUE);
    var operand2 = new ConditionExpression(EQUAL, "lastName", "Kshatriya", NEW_VALUE);
    ConditionExpression expression = new ConditionExpression(operand1, operand2, AND);

    var entity = stubContactDetail();
    entity.setFirstName("Om");
    entity.setLastName("Kshatriya");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isFalse();
  }

  @Test
  public void givenOrOperator_onlyOneEvaluatingTrue_evaluatesTrue() {
    var operand1 = new ConditionExpression(EQUAL, "firstName", "Yash", NEW_VALUE);
    var operand2 = new ConditionExpression(EQUAL, "lastName", "Kshatriya", NEW_VALUE);
    ConditionExpression expression = new ConditionExpression(operand1, operand2, OR);

    var entity = stubContactDetail();
    entity.setFirstName("Om");
    entity.setLastName("Kshatriya");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
  }

  @Test
  public void givenNotEquals_onDifferentNumbers_evaluatesTrue() {
    var expression = new ConditionExpression(NOT_EQUAL, "id", 501, NEW_VALUE);
    var entity = stubContactDetail();
    entity.setId(500L);

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
  }


  private ContactDetail stubContactDetail() {
    var contactDetail = new ContactDetail();
    contactDetail.setId(2000L);
    contactDetail.setFirstName("Steve");
    contactDetail.setLastName("Rogers");
    contactDetail.setName("Steve Rogers");
    contactDetail.setCity("Pune");
    return contactDetail;
  }

}