package com.kylas.sales.workflow.domain.workflow.action;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.BooleanUtils.toBooleanObject;
import static org.apache.commons.lang3.math.NumberUtils.createNumber;
import static org.apache.commons.lang3.math.NumberUtils.isParsable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Converter
@Slf4j
public class ValueConverter implements AttributeConverter<Object, String> {

  private static ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(Object attribute) {
    if (attribute instanceof Number || attribute instanceof Boolean) {
      return String.valueOf(attribute);
    }
    if (attribute instanceof String) {
      return "'" + attribute + "'";
    }
    try {
      return objectMapper.writeValueAsString(attribute);
    } catch (JsonProcessingException e) {
      log.error("error while converting object to JSON String. {}", attribute);
    }
    return String.valueOf(attribute);
  }

  @Override
  public Object convertToEntityAttribute(String dbData) {
    return getJsonValue(dbData);
  }

  public Object getValue(EditPropertyAction editPropertyAction, Field field) {
    switch (editPropertyAction.getValueType()) {
      case ARRAY:
        return convertToListOrArray(editPropertyAction.getValue(), field);
      case OBJECT:
        return convertToObject(editPropertyAction.getValue(), field);
      case PLAIN:
        return convertToWrapper(editPropertyAction.getValue(), field);
    }
    return field.getType();
  }

  private static List<?> convertToListOrArray(Object value, Field field) {
    try {
      if (field.getType().isAssignableFrom(List.class)) {
        ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, (Class<?>) parameterizedType.getActualTypeArguments()[0]);
        return objectMapper.readValue(String.valueOf(value), type);
      }
      if (field.getType().isArray()) {
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, field.getType().getComponentType());
        return objectMapper.readValue(String.valueOf(value), type);
      }
    } catch (JsonProcessingException e) {
      log.error("error in converting to list or array due to invalid json {}", value, e);
    }
    return emptyList();
  }

  private static Object convertToObject(Object value, Field field) {
    try {
      return objectMapper.readValue(String.valueOf(value), field.getType());
    } catch (JsonProcessingException e) {
      log.error("error while converting object to JSON String. {}", value);
    }
    return value;
  }

  private static Object convertToWrapper(Object value, Field field) {
    Class<?> fieldType = field.getType();
    String stringValue = String.valueOf(value);
    if (fieldType.isAssignableFrom(String.class)) {
      return stringValue;
    }
    if (fieldType.isAssignableFrom(Long.class)) {
      return createNumber(stringValue);
    }
    if (fieldType.isAssignableFrom(Double.class)) {
      return createNumber(stringValue);
    }
    if (fieldType.isAssignableFrom(Integer.class)) {
      return createNumber(stringValue);
    }
    if (fieldType.isAssignableFrom(Float.class)) {
      return createNumber(stringValue);
    }
    if (fieldType.isAssignableFrom(Boolean.class)) {
      return toBooleanObject(stringValue);
    }
    return value;
  }

  private static Object getJsonValue(String dbData) {
    try {
      return objectMapper.readTree(dbData);
    } catch (JsonProcessingException e) {
      log.error("{} is not a valid json", dbData);
      return !dbData.startsWith("'") && !dbData.endsWith("'") && isParsable(dbData) ? createNumber(dbData)
          : convertEntityAttributeToBoolean(dbData);
    }
  }

  private static Object convertEntityAttributeToBoolean(String dbData) {
    return dbData.equalsIgnoreCase("true") || dbData.equalsIgnoreCase("false") ? toBooleanObject(dbData) : dbData.replace("'", "");
  }

}