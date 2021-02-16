package com.kylas.sales.workflow.mq.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kylas.sales.workflow.domain.processor.task.Metadata;
import com.kylas.sales.workflow.domain.processor.task.TaskRelation;
import java.util.Date;
import java.util.Set;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateTaskEvent {

  private final String name;
  private final String description;
  private final Long priority;
  private final String outcome;
  private final Long type;
  private final Long status;
  private final Long assignedTo;
  private final Set<TaskRelation> relation;
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  private final Date dueDate;
  private final Metadata metadata;

  public CreateTaskEvent(@JsonProperty("name") String name, @JsonProperty("description") String description, @JsonProperty("priority") Long priority,
      @JsonProperty("outcome") String outcome, @JsonProperty("type") Long type,
      @JsonProperty("status") Long status,
      @JsonProperty("assignedTo") Long assignedTo,
      @JsonProperty("relation") Set<TaskRelation> relation, @JsonProperty("dueDate") Date dueDate,
      @JsonProperty("metadata") Metadata metadata) {
    this.name = name;
    this.description = description;
    this.priority = priority;
    this.outcome = outcome;
    this.type = type;
    this.status = status;
    this.assignedTo = assignedTo;
    this.relation = relation;
    this.dueDate = dueDate;
    this.metadata = metadata;
  }

  public static String getEventName() {
    return "workflow.task.create";
  }

}
