package com.kylas.sales.workflow.layout.api.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LayoutItem {

  private LayoutItemType type;
  List<LayoutItem> layoutItems;
  Object item;
  private int row;
  private int column;
  private int width;
  private Long id;
}
