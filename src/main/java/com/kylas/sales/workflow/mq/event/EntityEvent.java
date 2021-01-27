package com.kylas.sales.workflow.mq.event;

import com.kylas.sales.workflow.domain.processor.Actionable;
import com.kylas.sales.workflow.domain.processor.EntityDetail;

public interface EntityEvent {

  EntityDetail getEntity();

  EntityDetail getOldEntity();

  Metadata getMetadata();

  Actionable getActualEntity();

  long getEntityId();
}
