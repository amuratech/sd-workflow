package com.kylas.sales.workflow.api;

import com.kylas.sales.workflow.api.request.WorkflowRequest;
import com.kylas.sales.workflow.api.response.WorkflowSummary;
import com.kylas.sales.workflow.domain.WorkflowFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class WorkflowService {
  private final WorkflowFacade workflowFacade;

  @Autowired
  public WorkflowService(WorkflowFacade workflowFacade) {
    this.workflowFacade = workflowFacade;
  }

  public Mono<WorkflowSummary> create(WorkflowRequest workflowRequest) {
    return workflowFacade.create(workflowRequest).map(workflow -> new WorkflowSummary(workflow.getId()));
  }
}
