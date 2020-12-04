package com.kylas.sales.workflow.mq.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.kylas.sales.workflow.domain.workflow.EntityType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MetadataTest {

  @Test
  public void givenNonExecutedWorkflow_shouldCreateMetadata() {
    //given
    Metadata metadata = new Metadata(100L, 101L, EntityType.LEAD, null,null,EntityAction.CREATED);
    Long workflowId = 99L;
    //when
    Metadata newMetadata = metadata.with(workflowId);
    //then
    assertThat(newMetadata.getWorkflowId()).isEqualTo("WF_99");
    assertThat(newMetadata.getExecutedWorkflows()).isEmpty();
  }

  @Test
  public void givenExecutedWorkflow_shouldCreateMetadata() {
    //given
    Set<String> executedWorkflows = new HashSet<>();
    executedWorkflows.add("WF_9");
    Metadata metadata = new Metadata(100L, 101L, EntityType.LEAD,"WF_6", executedWorkflows, EntityAction.CREATED);
    Long workflowId = 99L;
    //when
    Metadata newMetadata = metadata.with(workflowId);
    //then
    assertThat(newMetadata.getWorkflowId()).isEqualTo("WF_99");
    assertThat(newMetadata.getExecutedWorkflows()).containsExactlyInAnyOrderElementsOf(Arrays.asList("WF_9", "WF_6"));
  }

  @Test
  public void givenExecutedWorkflowId_tryToCheckProcessed_shouldReturnTrue(){
    //given
    Long workflowId = 99L;
    Set<String> executedWorkflows = new HashSet<>();
    executedWorkflows.add("WF_99");
    Metadata metadata = new Metadata(100L, 101L, EntityType.LEAD,"WF_6", executedWorkflows, EntityAction.CREATED);
    //when & then
    assertThat(metadata.isProcessed("",workflowId)).isTrue();
  }

  @Test
  public void givenNonExecutedWorkflowId_tryToCheckProcessed_shouldReturnFalse(){
    //given
    Long workflowId = 99L;
    Set<String> executedWorkflows = new HashSet<>();
    executedWorkflows.add("WF_9");
    Metadata metadata = new Metadata(100L, 101L, EntityType.LEAD,"WF_6", executedWorkflows, EntityAction.CREATED);
    //when & then
    assertThat(metadata.isProcessed("",workflowId)).isFalse();
  }
  @Test
  public void givenPreviousExecutedWorkflow_tryToCheckWithCurrentWorkflowId_shouldReturnTrue(){
    //given
    Long workflowId = 99L;
    Set<String> executedWorkflows = new HashSet<>();
    Metadata metadata = new Metadata(100L, 101L, EntityType.LEAD,"WF_6", executedWorkflows, EntityAction.CREATED);
    //when & then
    assertThat(metadata.isProcessed("WF_99",workflowId)).isTrue();
  }

}