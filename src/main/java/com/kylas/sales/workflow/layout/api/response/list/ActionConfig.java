package com.kylas.sales.workflow.layout.api.response.list;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActionConfig {
  private boolean search;
  private boolean filter;
  private boolean create;
  private boolean importItems;
  private boolean columnSelector;
}
