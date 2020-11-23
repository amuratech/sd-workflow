package com.kylas.sales.workflow.domain.workflow.action.webhook;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Parameter {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @NotEmpty(message = "Parameter name must not be empty.")
  private String name;
  @NotEmpty(message = "Parameter entity must not be empty.")
  private String entity;
  @NotEmpty(message = "Parameter attribute must not be empty.")
  private String attribute;

  public Parameter(@NotEmpty(message = "Parameter name must not be empty.") String name,
      @NotEmpty(message = "Parameter entity must not be empty.") String entity,
      @NotEmpty(message = "Parameter attribute must not be empty.") String attribute) {
    this.name = name;
    this.entity = entity;
    this.attribute = attribute;
  }
}
