package com.kylas.sales.workflow.domain.service;

import static java.util.Objects.isNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylas.sales.workflow.domain.exception.InvalidActionException;
import com.kylas.sales.workflow.domain.processor.lead.IdName;
import com.kylas.sales.workflow.domain.user.User;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class ValueResolver {

  private final PipelineService pipelineService;
  private final ProductService productService;
  private final ObjectMapper objectMapper;
  private final UserService userService;

  @Autowired
  public ValueResolver(PipelineService pipelineService, ProductService productService, ObjectMapper objectMapper,
      UserService userService) {
    this.pipelineService = pipelineService;
    this.productService = productService;
    this.objectMapper = objectMapper;
    this.userService = userService;
  }

  public Mono<IdName> getPipeline(Object pipeline, String authenticationToken) {
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

  public Mono<IdName> getPipelineStage(Object pipelineStage, String authenticationToken) {
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

  public Mono<IdName> getProduct(Object product, String authenticationToken) {
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

  public Mono<IdName> getUser(Object user, String authenticationToken) {
    if (isNull(user)) {
      return Mono.empty();
    }
    try {
      var userIdName = objectMapper.readValue(serialize(user), IdName.class);
      return userService.getUserDetails(userIdName.getId(), authenticationToken)
          .map(userResponse -> new IdName(userResponse.getId(), userResponse.getName()));
    } catch (JsonProcessingException e) {
      log.error("Exception while extracting userId from {}", user);
      throw new InvalidActionException();
    }
  }

  public IdName getIdNameFrom(Object value) {
    try {
      return objectMapper.readValue(serialize(value), IdName.class);
    } catch (JsonProcessingException e) {
      log.error("Exception while extracting IdName from {}", value);
      throw new IllegalArgumentException();
    }
  }

  public String serialize(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      log.error("Exception while serializing value {}", value);
      throw new IllegalArgumentException();
    }
  }

  public List getListFrom(String value) {
    try {
      return objectMapper.readValue(value, List.class);
    } catch (JsonProcessingException e) {
      log.error("Exception while serializing value {}", value);
      throw new IllegalArgumentException();
    }
  }

  public Mono<String> getUserName(Long userId, String authenticationToken) {
    return userService.getUserDetails(userId, authenticationToken).map(User::getName);
  }
}
