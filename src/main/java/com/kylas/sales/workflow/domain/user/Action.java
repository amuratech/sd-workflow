package com.kylas.sales.workflow.domain.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Setter;

@Setter
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class Action implements Serializable {

  private boolean read;
  private boolean update;
  private boolean write;
  private boolean readAll;
  private boolean updateAll;
}
