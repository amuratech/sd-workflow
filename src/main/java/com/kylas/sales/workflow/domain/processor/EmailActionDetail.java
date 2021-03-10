package com.kylas.sales.workflow.domain.processor;

import com.kylas.sales.workflow.domain.processor.lead.Email;
import com.kylas.sales.workflow.domain.processor.lead.IdName;
import java.util.List;
import lombok.Getter;

@Getter
public class EmailActionDetail {

  private final String name;
  private final IdName createdBy;
  private final IdName updatedBy;
  private final IdName ownedBy;
  private final Email[] emails;
  private final List<IdName> associatedContacts;


  public EmailActionDetail(String name, IdName createdBy, IdName updatedBy, IdName ownedBy, Email[] emails,
      List<IdName> associatedContacts) {
    this.name = name;
    this.createdBy = createdBy;
    this.updatedBy = updatedBy;
    this.ownedBy = ownedBy;
    this.emails = emails;
    this.associatedContacts = associatedContacts;
  }
}
