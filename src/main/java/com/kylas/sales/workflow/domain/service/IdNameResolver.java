package com.kylas.sales.workflow.domain.service;

import static java.util.Objects.isNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylas.sales.workflow.domain.exception.InvalidActionException;
import com.kylas.sales.workflow.domain.processor.lead.IdName;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class IdNameResolver {

  private static PipelineService pipelineService;
  private static ObjectMapper objectMapper;

  public static Mono<IdName> getPipeline(Object pipeline, String authenticationToken) {
    if (isNull(pipeline)) {
      return Mono.empty();
    }
    try {
      var pipelineIdName = objectMapper.readValue(String.valueOf(pipeline), IdName.class);
      return pipelineService.getPipeline(pipelineIdName.getId(), authenticationToken);
    } catch (JsonProcessingException e) {
      log.error("Exception while extracting pipelineId from {}", pipeline);
      throw new InvalidActionException();
    }
  }

  public static IdName getIdNameFrom(Object value) {
    try {
      return objectMapper.readValue(String.valueOf(value), IdName.class);
    } catch (JsonProcessingException e) {
      log.error("Exception while extracting IdName from {}", value);
      throw new IllegalArgumentException();
    }
  }

  public static String serialize(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      log.error("Exception while serializing value {}", value);
      throw new IllegalArgumentException();
    }
  }

  @Autowired
  public void setPipelineService(PipelineService pipelineService) {
    IdNameResolver.pipelineService = pipelineService;
  }

  @Autowired
  public void setObjectMapper(ObjectMapper objectMapper) {
    IdNameResolver.objectMapper = objectMapper;
  }
}
