package com.kylas.sales.workflow.domain.workflow.action.email;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Participant {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @JsonIgnore
  private UUID id;
  @NotNull
  @Enumerated(EnumType.STRING)
  private EmailActionType type;
  @NotBlank
  private String entity;
  private Long entityId;
  private String name;
  private String email;

  public Participant(@NotNull EmailActionType type, @NotBlank String entity, Long entityId, String name, String email) {
    this.type = type;
    this.entity = entity;
    this.entityId = entityId;
    this.name = name;
    this.email = email;
  }
}
