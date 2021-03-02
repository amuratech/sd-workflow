package com.kylas.sales.workflow.domain.service;

import static java.util.Objects.isNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylas.sales.workflow.common.dto.condition.IdNameField;
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
  private final CompanyService companyService;

  @Autowired
  public ValueResolver(PipelineService pipelineService, ProductService productService, ObjectMapper objectMapper,
      UserService userService, CompanyService companyService) {
    this.pipelineService = pipelineService;
    this.productService = productService;
    this.objectMapper = objectMapper;
    this.userService = userService;
    this.companyService = companyService;
  }

  public Mono<IdName> resolveNamesOfIdNameFieldsExceptUserFields(String fieldName, Object value, String authentication) {

    if (isNull(value)) {
      return Mono.empty();
    }

    IdNameField field = IdNameField.getFieldByName(fieldName);
    try {
      var idName = objectMapper.readValue(serialize(value), IdName.class);
      switch (field) {
        case PIPELINE:
          return pipelineService.getPipeline(idName.getId(), authentication);
        case PIPELINE_STAGE:
          return pipelineService.getPipelineStage(idName.getId(), authentication);
        case PRODUCT:
          return productService.getProduct(idName.getId(), authentication);
        case COMPANY:
          return companyService.getCompanyById(idName.getId(), authentication);
      }
    } catch (JsonProcessingException e) {
      log.error("error in parsing json", e);
    }
    throw new InvalidActionException();
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
