package com.kylas.sales.workflow.domain;

import static com.kylas.sales.workflow.api.request.Condition.TriggerType.NEW_VALUE;
import static com.kylas.sales.workflow.common.dto.condition.Operator.AND;
import static com.kylas.sales.workflow.common.dto.condition.Operator.EQUAL;
import static com.kylas.sales.workflow.common.dto.condition.Operator.IS_NOT_NULL;
import static com.kylas.sales.workflow.common.dto.condition.Operator.OR;
import static org.assertj.core.api.Assertions.assertThat;

import com.kylas.sales.workflow.api.request.Condition.ExpressionElement;
import com.kylas.sales.workflow.common.dto.condition.Operator;
import com.kylas.sales.workflow.common.dto.condition.WorkflowCondition.ConditionExpression;
import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(initializers = {TestDatabaseInitializer.class})
class ConditionFacadeTest {

  @Autowired
  private ConditionFacade conditionFacade;

  @Nested
  @DisplayName("Expression building verification")
  public class BuildExpressionTests {

    @Test
    public void givenTwoAndElements_shouldBuildExpression() {
      //given
      var andOperator = stubOperator(AND);
      var operand1 = stubEqualConditionElement("firstName", "Tony");
      var operand2 = stubEqualConditionElement("lastName", "Stark");
      var elements = List.of(operand1, andOperator, operand2);

      //when
      var expression = conditionFacade.buildExpression(elements);

      //then
      assertThat(expression).isNotNull();
      assertThat(areEquivalent(operand1, expression.getOperand1())).isTrue();
      assertThat(areEquivalent(operand2, expression.getOperand2())).isTrue();
      assertThat(expression.getOperator()).isEqualTo(AND);
    }

    @Test
    public void givenThreeAndElements_shouldBuildExpression() {
      //given
      var andOperator = stubOperator(AND);
      var operand1 = stubEqualConditionElement("firstName", "Tony");
      var operand2 = stubEqualConditionElement("firstName", "Bran");
      var operand3 = stubEqualConditionElement("firstName", "Brian");
      var elements = List.of(operand1, andOperator, operand2, andOperator, operand3);

      //when
      var expression = conditionFacade.buildExpression(elements);

      //then
      assertThat(expression).isNotNull();
      assertThat(areEquivalent(operand1, expression.getOperand1().getOperand1())).isTrue();
      assertThat(areEquivalent(operand2, expression.getOperand1().getOperand2())).isTrue();
      assertThat(expression.getOperand1().getOperator()).isEqualTo(AND);
      assertThat(areEquivalent(operand3, expression.getOperand2())).isTrue();
      assertThat(expression.getOperator()).isEqualTo(AND);
    }

    @Test
    public void givenTwoOrElements_shouldBuildExpression() {
      //given
      var orOperator = stubOperator(OR);
      var operand1 = stubEqualConditionElement("firstName", "Jack");
      var operand2 = stubEqualConditionElement("firstName", "Sam");
      var elements = List.of(operand1, orOperator, operand2);

      //when
      var expression = conditionFacade.buildExpression(elements);

      //then
      assertThat(expression).isNotNull();
      assertThat(areEquivalent(operand1, expression.getOperand1())).isTrue();
      assertThat(areEquivalent(operand2, expression.getOperand2())).isTrue();
      assertThat(expression.getOperator()).isEqualTo(OR);
    }

    @Test
    public void givenThreeOrElements_shouldBuildExpression() {
      //given
      var orOperator = stubOperator(OR);
      var operand1 = stubEqualConditionElement("firstName", "Tim");
      var operand2 = stubEqualConditionElement("firstName", "Jackson");
      var operand3 = stubEqualConditionElement("lastName", "Tonks");
      var elements = List.of(operand1, orOperator, operand2, orOperator, operand3);

      //when
      var expression = conditionFacade.buildExpression(elements);

      //then
      assertThat(expression).isNotNull();
      assertThat(areEquivalent(operand1, expression.getOperand1().getOperand1())).isTrue();
      assertThat(areEquivalent(operand2, expression.getOperand1().getOperand2())).isTrue();
      assertThat(expression.getOperand1().getOperator()).isEqualTo(OR);
      assertThat(areEquivalent(operand3, expression.getOperand2())).isTrue();
      assertThat(expression.getOperator()).isEqualTo(OR);
    }

    @Test
    public void givenTwoAndElements_separatedByOr_shouldBuildExpression() {
      //given
      var orOperator = stubOperator(OR);
      var andOperator = stubOperator(AND);
      var operand1 = stubEqualConditionElement("firstName", "Tony1");
      var operand2 = stubEqualConditionElement("firstName", "Tony2");
      var operand3 = stubEqualConditionElement("firstName", "Tony3");
      var operand4 = stubEqualConditionElement("firstName", "Tony4");
      var elements =
          List.of(operand1, andOperator, operand2, orOperator, operand3, andOperator, operand4);

      //when
      var expression = conditionFacade.buildExpression(elements);

      //then
      assertThat(expression).isNotNull();
      assertThat(expression.getOperator()).isEqualTo(OR);

      var leftChild = expression.getOperand1();
      assertThat(leftChild.getOperator()).isEqualTo(AND);
      assertThat(areEquivalent(operand1, leftChild.getOperand1())).isTrue();
      assertThat(areEquivalent(operand2, leftChild.getOperand2())).isTrue();

      var rightChild = expression.getOperand2();
      assertThat(rightChild.getOperator()).isEqualTo(AND);
      assertThat(areEquivalent(operand3, rightChild.getOperand1())).isTrue();
      assertThat(areEquivalent(operand4, rightChild.getOperand2())).isTrue();
    }

    @Test
    public void givenTwoOrElements_separatedByAnd_shouldBuildExpression() {
      //given
      var orOperator = stubOperator(OR);
      var andOperator = stubOperator(AND);
      var operand1 = stubEqualConditionElement("firstName", "Tony1");
      var operand2 = stubEqualConditionElement("firstName", "Tony2");
      var operand3 = stubEqualConditionElement("firstName", "Tony3");
      var operand4 = stubEqualConditionElement("firstName", "Tony4");
      var elements =
          List.of(operand1, orOperator, operand2, andOperator, operand3, orOperator, operand4);

      //when
      var expression = conditionFacade.buildExpression(elements);

      //then
      assertThat(expression).isNotNull();
      assertThat(expression.getOperator()).isEqualTo(OR);

      assertThat(expression.getOperand1().getOperator()).isEqualTo(OR);
      assertThat(areEquivalent(operand4, expression.getOperand2())).isTrue();

      var andExpression = expression.getOperand1().getOperand2();
      assertThat(andExpression.getOperator()).isEqualTo(AND);
      assertThat(areEquivalent(operand2, andExpression.getOperand1())).isTrue();
      assertThat(areEquivalent(operand3, andExpression.getOperand2())).isTrue();
    }
  }

  @Nested
  @DisplayName("Expression flattening verification")
  public class FlattenExpressionTests {

    @Test
    public void givenAndExpression_shouldFlatten() {
      //given
      var operand1 = stubEqualExpression("firstName", "Tony1");
      var operand2 = stubEqualExpression("lastName", "Stark1");
      var expression = new ConditionExpression(operand1, operand2, AND);

      //when
      List<ExpressionElement> elements = conditionFacade.flattenExpression(expression);

      //then
      assertThat(elements.get(0)).satisfies(e -> areEquivalent(e, operand1));
      assertThat(elements.get(1)).satisfies(e -> e.getOperator().equals(AND));
      assertThat(elements.get(2)).satisfies(e -> areEquivalent(e, operand2));
    }

    @Test
    public void givenOrExpression_shouldFlatten() {
      //given
      var operand1 = stubEqualExpression("firstName", "Tony1");
      var operand2 = stubEqualExpression("lastName", "Stark1");
      var expression = new ConditionExpression(operand1, operand2, OR);

      //when
      List<ExpressionElement> elements = conditionFacade.flattenExpression(expression);

      //then
      assertThat(elements.get(0)).satisfies(e -> areEquivalent(e, operand1));
      assertThat(elements.get(1)).satisfies(e -> e.getOperator().equals(OR));
      assertThat(elements.get(2)).satisfies(e -> areEquivalent(e, operand2));
    }

    @Test
    public void givenAndExpression_withThreeConditions_shouldFlatten() {
      //given
      var operand1 = stubEqualExpression("firstName", "Tony1");
      var operand2 = stubEqualExpression("firstName", "Tony2");
      var leftChild = new ConditionExpression(operand1, operand2, AND);
      var rightChild = stubEqualExpression("firstName", "Tony3");
      var expression = new ConditionExpression(leftChild, rightChild, AND);
      //when
      List<ExpressionElement> elements = conditionFacade.flattenExpression(expression);

      //then
      assertThat(elements.get(0)).satisfies(element -> areEquivalent(element, operand1));
      assertThat(elements.get(1)).satisfies(element -> element.getOperator().equals(AND));
      assertThat(elements.get(2)).satisfies(element -> areEquivalent(element, operand2));
      assertThat(elements.get(3)).satisfies(element -> element.getOperator().equals(AND));
      assertThat(elements.get(4)).satisfies(element -> areEquivalent(element, rightChild));
    }

    @Test
    public void givenTwoAndExpressions_SeparatedByOr_shouldFlatten() {
      //given
      var expressionA = stubEqualExpression("firstName", "Tony1");
      var expressionB = stubEqualExpression("firstName", "Tony2");
      var leftChild = new ConditionExpression(expressionA, expressionB, AND);

      var expressionC = stubEqualExpression("firstName", "Tony3");
      var expressionD = stubEqualExpression("firstName", "Tony4");
      var rightChild = new ConditionExpression(expressionC, expressionC, AND);

      var expression = new ConditionExpression(leftChild, rightChild, OR);
      //when
      List<ExpressionElement> elements = conditionFacade.flattenExpression(expression);

      //then
      assertThat(elements.get(0)).satisfies(element -> areEquivalent(element, expressionA));
      assertThat(elements.get(1)).satisfies(element -> element.getOperator().equals(AND));
      assertThat(elements.get(2)).satisfies(element -> areEquivalent(element, expressionB));
      assertThat(elements.get(3)).satisfies(element -> element.getOperator().equals(OR));
      assertThat(elements.get(4)).satisfies(element -> areEquivalent(element, expressionC));
      assertThat(elements.get(5)).satisfies(element -> element.getOperator().equals(AND));
      assertThat(elements.get(6)).satisfies(element -> areEquivalent(element, expressionD));
    }
  }

  @Nested
  @DisplayName("Condition evaluation tests")
  public class ConditionEvaluationTests {

    @Test
    public void givenIsSetOperator_havingNoExpressionValue_shouldResolveExpression() {
      //when
      var expression = new ConditionExpression(IS_NOT_NULL, "ownerId", null, NEW_VALUE);
      Mono<ConditionExpression> expressionMono = conditionFacade.nameResolved(expression, "someToken");

      //then
      StepVerifier
          .create(expressionMono)
          .assertNext(received -> {
            assertThat(received).isNotNull();
            assertThat(received.getOperator()).isEqualTo(IS_NOT_NULL);
            assertThat(received.getName()).isEqualTo("ownerId");
            assertThat(received.getValue()).isEqualTo(null);
          })
          .verifyComplete();
    }
  }

  private ConditionExpression stubEqualExpression(String name, String value) {
    return new ConditionExpression(EQUAL, name, value, NEW_VALUE);
  }

  private boolean areEquivalent(ExpressionElement element, ConditionExpression expression) {
    return element.getOperator().equals(expression.getOperator())
        && element.getName().equals(expression.getName())
        && element.getValue().equals(expression.getValue())
        && element.getTriggerOn().equals(expression.getTriggerOn());
  }

  @NotNull
  private ExpressionElement stubEqualConditionElement(String name, String value) {
    return new ExpressionElement("EQUAL", name, value, "NEW_VALUE");
  }

  @NotNull
  private ExpressionElement stubOperator(Operator operator) {
    return new ExpressionElement(operator.name(), null, null, null);
  }
}