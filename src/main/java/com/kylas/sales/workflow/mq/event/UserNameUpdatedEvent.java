package com.kylas.sales.workflow.mq.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserNameUpdatedEvent {

  private final long tenantId;
  private final long userId;
  private final String firstName;
  private final String lastName;

  @JsonCreator
  public UserNameUpdatedEvent(@JsonProperty("tenantId") long tenantId, @JsonProperty("userId") long userId,
      @JsonProperty("firstName") String firstName, @JsonProperty("lastName") String lastName) {
    this.tenantId = tenantId;
    this.userId = userId;
    this.firstName = firstName;
    this.lastName = lastName;
  }

  @JsonIgnore
  public static String getEventName() {
    return "user.name.updated";
  }
}
