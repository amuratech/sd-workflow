package com.kylas.sales.workflow.domain.workflow.action.task;

import com.kylas.sales.workflow.domain.processor.task.AssignedToType;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
public class AssignedTo {

  @Enumerated(value = EnumType.STRING)
  private AssignedToType type;
  private Long id;
  private String name;

  public AssignedTo(AssignedToType type, Long id, String name) {
    this.type = type;
    this.id = id;
    this.name = name;
  }
}
