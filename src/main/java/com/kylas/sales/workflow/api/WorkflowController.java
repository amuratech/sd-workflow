package com.kylas.sales.workflow.api;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.kylas.sales.workflow.api.request.WorkflowRequest;
import com.kylas.sales.workflow.api.response.WorkflowDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/v1/workflows")
public class WorkflowController {

  private final WorkflowService workflowService;

  @Autowired
  public WorkflowController(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  @PostMapping(value = "", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity> createWorkflow(@RequestBody WorkflowRequest workflowRequest) {
    return workflowService.create(workflowRequest)
        .map(workflowSummary -> {
          var workflowDetailUri = UriComponentsBuilder.fromPath("/{id}")
              .buildAndExpand(workflowSummary.getId())
              .toUri();
          return ResponseEntity.created(workflowDetailUri).body(workflowSummary);
        });
  }

  @GetMapping(value = "/{id}", produces = APPLICATION_JSON_VALUE)
  public ResponseEntity getWorkflow(@PathVariable("id") long workflowId) {
    return ResponseEntity.ok(workflowService.get(workflowId));
  }

  @PostMapping(value = "/list", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  public Mono<Page<WorkflowDetail>> getAllWorkflow(@PageableDefault(page = 0, size = 10) Pageable pageable) {
    return workflowService.list(pageable);
  }

  @PostMapping(value = "/{id}/deactivate", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<WorkflowDetail> deactivate(@PathVariable("id") long workflowId) {
    return ResponseEntity.ok(workflowService.deactivate(workflowId));
  }

  @PostMapping(value = "/{id}/activate", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<WorkflowDetail> activate(@PathVariable("id") long workflowId) {
    return ResponseEntity.ok(workflowService.activate(workflowId));
  }

}
