package com.kylas.sales.workflow.mq.event;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kylas.sales.workflow.domain.processor.Actionable;
import com.kylas.sales.workflow.domain.processor.EmailActionDetail;
import com.kylas.sales.workflow.domain.processor.contact.Contact;
import com.kylas.sales.workflow.domain.processor.contact.ContactDetail;
import java.util.Collections;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContactEvent implements EntityEvent {

  private ContactDetail entity;
  private ContactDetail oldEntity;
  private Metadata metadata;

  @JsonCreator
  public ContactEvent(
      @JsonProperty("entity") ContactDetail entity,
      @JsonProperty("oldEntity") ContactDetail oldEntity,
      @JsonProperty("metadata") Metadata metadata
  ) {
    this.entity = entity;
    this.oldEntity = oldEntity;
    this.metadata = metadata;
  }

  public static String getContactCreatedEventName() {
    return "sales.contact.created.v2";
  }

  public static String getContactUpdatedEventName() {
    return "sales.contact.updated.v2";
  }

  @Override
  public ContactDetail getEntity() {
    return this.entity;
  }

  @Override
  public ContactDetail getOldEntity() {
    return this.oldEntity;
  }

  @Override
  public Metadata getMetadata() {
    return this.metadata;
  }

  @Override
  public Actionable getActualEntity() {
    return new Contact();
  }

  @Override
  public EmailActionDetail getEmailActionDetail() {
    return new EmailActionDetail(this.entity.getName(), this.entity.getCreatedBy(), this.entity.getUpdatedBy(), this.entity.getOwnerId(),
        this.entity.getEmails(),
        Collections
            .emptyList());
  }

  @Override
  public long getEntityId() {
    return this.entity.getId();
  }
}