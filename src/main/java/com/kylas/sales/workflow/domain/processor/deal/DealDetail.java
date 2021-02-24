package com.kylas.sales.workflow.domain.processor.deal;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.kylas.sales.workflow.domain.processor.Actionable;
import com.kylas.sales.workflow.domain.processor.EntityDetail;
import com.kylas.sales.workflow.domain.processor.lead.IdName;
import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@Setter
@JsonInclude(Include.NON_NULL)
public class DealDetail implements Actionable, EntityDetail {

  private Long id;
  private String name;
  private IdName ownedBy;
  private Money estimatedValue;
  private Money actualValue;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  private Date estimatedClosureOn;
  private List<IdName> associatedContacts;
  private IdName product;
  private Pipeline pipeline;
  private IdName company;
  private IdName createdBy;
  private IdName updatedBy;
  private Date createdAt;
  private Date updatedAt;

  @Override
  @JsonIgnore
  public String getEventName() {
    return "workflow.deal.update";
  }
}
