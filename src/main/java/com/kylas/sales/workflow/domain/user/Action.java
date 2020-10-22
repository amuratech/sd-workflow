package com.kylas.sales.workflow.domain.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Setter;

@Setter
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class Action implements Serializable {

  @JsonProperty("read")
  private boolean read;

  @JsonProperty("update")
  private boolean update;

  @JsonProperty("write")
  private boolean write;

  @JsonProperty("readAll")
  private boolean readAll;

  @JsonProperty("updateAll")
  private boolean updateAll;

  public boolean canRead() {
    return this.read;
  }

  public boolean canWrite() {
    return this.write;
  }

  public boolean canReadAll() {
    return readAll;
  }

  public boolean canUpdate() {
    return this.update;
  }

  public boolean canUpdateAll() {
    return this.updateAll;
  }
}
