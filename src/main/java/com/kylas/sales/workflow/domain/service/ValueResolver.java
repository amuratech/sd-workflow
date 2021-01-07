package com.kylas.sales.workflow.domain.service;

import static java.util.Objects.isNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylas.sales.workflow.domain.exception.InvalidActionException;
import com.kylas.sales.workflow.domain.processor.lead.IdName;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class ValueResolver {

  private static PipelineService pipelineService;
  private static ProductService productService;
  private static ObjectMapper objectMapper;

  public static Mono<IdName> getPipeline(Object pipeline, String authenticationToken) {
    if (isNull(pipeline)) {
      return Mono.empty();
    }
    try {
      var pipelineIdName = objectMapper.readValue(serialize(pipeline), IdName.class);
      return pipelineService.getPipeline(pipelineIdName.getId(), authenticationToken);
    } catch (JsonProcessingException e) {
      log.error("Exception while extracting pipelineId from {}", pipeline);
      throw new InvalidActionException();
    }
  }

  public static Mono<IdName> getPipelineStage(Object pipelineStage, String authenticationToken) {
    if (isNull(pipelineStage)) {
      return Mono.empty();
    }
    try {
      var pipelineStageIdName = objectMapper.readValue(serialize(pipelineStage), IdName.class);
      return pipelineService.getPipelineStage(pipelineStageIdName.getId(), authenticationToken);
    } catch (JsonProcessingException e) {
      log.error("Exception while extracting pipelineId from {}", pipelineStage);
      throw new InvalidActionException();
    }
  }
  public static Mono<IdName> getProduct(Object product, String authenticationToken) {
    if (isNull(product)) {
      return Mono.empty();
    }
    try {
      var productIdName = objectMapper.readValue(serialize(product), IdName.class);
      return productService.getProduct(productIdName.getId(), authenticationToken);
    } catch (JsonProcessingException e) {
      log.error("Exception while extracting pipelineId from {}", product);
      throw new InvalidActionException();
    }
  }

  public static IdName getIdNameFrom(Object value) {
    try {
      return objectMapper.readValue(serialize(value), IdName.class);
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

  public static List getListFrom(String value) {
    try {
      return objectMapper.readValue(value, List.class);
    } catch (JsonProcessingException e) {
      log.error("Exception while serializing value {}", value);
      throw new IllegalArgumentException();
    }
  }



  @Autowired
  public void setPipelineService(PipelineService pipelineService) {
    ValueResolver.pipelineService = pipelineService;
  }

  @Autowired
  public void setProductService(ProductService productService) {
    ValueResolver.productService = productService;
  }
  
  @Autowired
  public void setObjectMapper(ObjectMapper objectMapper) {
    ValueResolver.objectMapper = objectMapper;
  }
}
