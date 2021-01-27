package com.kylas.sales.workflow.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kylas.sales.workflow.domain.exception.InvalidFilterException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class WorkflowFilterTest {
  @Test
  public void givenBlankFieldName_shouldThrow() {
    assertThatThrownBy(
            () -> {
              new WorkflowFilter("equal", "", "string", "fieldValue");
            })
        .isInstanceOf(InvalidFilterException.class)
        .withFailMessage("01701006");
  }

  @Test
  public void givenBlankFieldType_shouldThrow() {
    assertThatThrownBy(
            () -> {
              new WorkflowFilter("equal", "status", "", "fieldValue");
            })
        .isInstanceOf(InvalidFilterException.class)
        .withFailMessage("01701006");
  }

  @Test
  public void givenBlankOperator_shouldThrow() {
    assertThatThrownBy(
            () -> {
              new WorkflowFilter("", "status", "string", "fieldValue");
            })
        .isInstanceOf(InvalidFilterException.class)
        .withFailMessage("01701006");
  }

  @Test
  public void givenInvalidEntity_shouldThrow() {
    assertThatThrownBy(
        () -> {
          new WorkflowFilter("equal", "entityType", "string", "lead1");
        })
        .isInstanceOf(InvalidFilterException.class)
        .withFailMessage("01701006");
  }

  @ParameterizedTest(name = "Operator \"{0}\" FieldValue {1}")
  @MethodSource("operatorAndValue")
  public void givenEachOperator_shouldThrow(String operator, Object fieldValue) {

    assertThatThrownBy(
            () -> {
              new WorkflowFilter(operator, "", "", fieldValue);
            })
        .isInstanceOf(InvalidFilterException.class)
        .hasMessage("01701006");
  }

  private static Stream<Arguments> operatorAndValue() {
    return Stream.of(
        Arguments.of("equal", null),
        Arguments.of("equal", ""),
        Arguments.of("not_equal", null),
        Arguments.of("not_equal", ""),
        Arguments.of("greater", null),
        Arguments.of("greater_or_equal", ""),
        Arguments.of("less", null),
        Arguments.of("less_or_equal", ""),
        Arguments.of("between", null),
        Arguments.of("between", ""),
        Arguments.of("between", Arrays.asList("2020-01-11T10:22:40.000z")),
        Arguments.of("between", Arrays.asList("", "")),
        Arguments.of("not_between", null),
        Arguments.of("not_between", ""),
        Arguments.of("not_between", Arrays.asList("2020-01-11T10:22:40.000z")),
        Arguments.of("not_between", Arrays.asList("", "")));
  }

}
