package com.kylas.sales.workflow.layout.api.response.list;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ListLayout {
  private boolean leftNav;
  private PageConfig pageConfig;

  @JsonProperty("defaultConfig")
  private DefaultColumns defaultFields;

  @JsonCreator
  public ListLayout(
      @JsonProperty("leftNav") boolean leftNav,
      @JsonProperty("pageConfig") PageConfig pageConfig,
      @JsonProperty("defaultConfig") DefaultColumns defaultFields) {
    this.leftNav = leftNav;
    this.pageConfig = pageConfig;
    this.defaultFields = defaultFields;
  }

  @Getter
  public static class DefaultColumns {
    private final List<String> fields;

    @JsonCreator
    public DefaultColumns(@JsonProperty("fields") List<String> fields) {
      this.fields = fields;
    }
  }
}
