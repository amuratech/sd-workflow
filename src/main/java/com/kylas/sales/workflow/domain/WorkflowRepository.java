package com.kylas.sales.workflow.domain;

import com.kylas.sales.workflow.common.dto.UsageRecord;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
interface WorkflowRepository extends JpaRepository<Workflow, Long>, JpaSpecificationExecutor<Workflow> {

  @Query("SELECT new com.kylas.sales.workflow.common.dto.UsageRecord(w.tenantId, COUNT(w)) "
      + "FROM Workflow w "
      + "WHERE w.active=TRUE "
      + "GROUP BY w.tenantId")
  List<UsageRecord> getActiveCountByTenantId();
}
