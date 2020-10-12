package com.kylas.sales.workflow.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.kylas.sales.workflow.api.request.WorkflowRequest;
import com.kylas.sales.workflow.api.response.WorkflowSummary;
import com.kylas.sales.workflow.domain.WorkflowFacade;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

  @InjectMocks
  private WorkflowService workflowService;

  @Mock
  private WorkflowFacade workflowFacade;

  @Test
  public void givenWorkflowRequest_shouldCreateIt() {
    //given
    var workflowRequestMock = mock(WorkflowRequest.class);
    var workflowMock = mock(Workflow.class);
    given(workflowMock.getId()).willReturn(1L);
    given(workflowFacade.create(workflowRequestMock)).willReturn(Mono.just(workflowMock));
    //when
    Mono<WorkflowSummary> workflowSummaryMono = workflowService.create(workflowRequestMock);
    //then
    StepVerifier.create(workflowSummaryMono)
        .assertNext(workflowSummary -> assertThat(workflowMock.getId()).isEqualTo(1L))
        .verifyComplete();
  }
}