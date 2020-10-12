package com.kylas.sales.workflow.api;

import com.kylas.sales.workflow.api.request.WorkflowRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/v1/workflows")
public class WorkflowController {

  private final WorkflowService workflowService;

  @Autowired
  public WorkflowController(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  @PostMapping(value = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity> createWorkflow(@RequestBody WorkflowRequest workflowRequest) {
    return workflowService.create(workflowRequest)
        .map(workflowSummary -> ResponseEntity.ok(workflowSummary));
  }

}
