package com.kylas.sales.workflow.domain.workflow.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

@Converter
@Slf4j
public class ValueConverter implements AttributeConverter<Object, String> {

  @Override
  public String convertToDatabaseColumn(Object attribute) {
    ObjectMapper objectMapper = new ObjectMapper();
    if (attribute instanceof Number || attribute instanceof String) {
      return String.valueOf(attribute);
    }
    try {
      return objectMapper.writeValueAsString(attribute);
    } catch (JsonProcessingException e) {
      log.error("error while converting object to JSON String.", e);
    }
    return String.valueOf(attribute);
  }

  @Override
  public Object convertToEntityAttribute(String dbData) {
    return dbData;
  }
}