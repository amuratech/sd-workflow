package com.kylas.sales.workflow.common.dto.condition;

import com.kylas.sales.workflow.domain.exception.InvalidWorkflowPropertyException;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IdNameField {

  PIPELINE("pipeline"),
  PIPELINE_STAGE("pipelineStage"),
  PRODUCT("product"),
  COMPANY("company");

  private final String name;

  public static IdNameField getFieldByName(String name) {
    return Arrays.stream(values())
        .filter(idNameField -> idNameField.getName().equals(name))
        .findFirst()
        .orElseThrow(InvalidWorkflowPropertyException::new);
  }
}
