package com.kylas.sales.workflow.domain.workflow.action.task;

import javax.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
public class DueDate {

  private int days;
  private int hours;

  public DueDate(int days, int hours) {
    this.days = days;
    this.hours = hours;
  }
}
