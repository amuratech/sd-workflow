package com.kylas.sales.workflow.domain;

import com.kylas.sales.workflow.domain.workflow.WorkflowExecutedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
interface WorkflowExecutedEventRepository extends JpaRepository<WorkflowExecutedEvent, Long>, JpaSpecificationExecutor<WorkflowExecutedEvent> {

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update WorkflowExecutedEvent w set w.lastTriggeredAt = now(), w.triggerCount = w.triggerCount+1 where w.id = :Id")
  void updateEventDetails(@Param("Id") long id);
}
