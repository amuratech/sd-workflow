package com.kylas.sales.workflow.domain.workflow.action;

import static java.lang.String.valueOf;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.BooleanUtils.toBooleanObject;
import static org.apache.commons.lang3.math.NumberUtils.createNumber;
import static org.apache.commons.lang3.math.NumberUtils.isParsable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylas.sales.workflow.domain.processor.lead.IdName;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Converter
@Slf4j
public class ValueConverter implements AttributeConverter<Object, String> {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(Object attribute) {
    if (attribute instanceof Number || attribute instanceof Boolean) {
      return valueOf(attribute);
    }
    if (attribute instanceof String) {
      return String.valueOf(attribute);
    }
    try {
      return objectMapper.writeValueAsString(attribute);
    } catch (JsonProcessingException e) {
      log.error("error while converting object to JSON String. {}", attribute);
    }
    return valueOf(attribute);
  }

  @Override
  public Object convertToEntityAttribute(String dbData) {
    return getJsonValue(dbData);
  }

  public Object getValue(EditPropertyAction editPropertyAction, Field field, EntityType entityType) {

    switch (editPropertyAction.getValueType()) {
      case ARRAY:
        return convertToListOrArray(editPropertyAction.getValue(), field);
      case OBJECT:
        return convertToObject(editPropertyAction, field, entityType);
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
        return objectMapper.readValue(valueOf(value), type);
      }
      if (field.getType().isArray()) {
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, field.getType().getComponentType());
        return objectMapper.readValue(valueOf(value), type);
      }
    } catch (JsonProcessingException e) {
      log.error("error in converting to list or array due to invalid json {}", value);
    }
    return emptyList();
  }

  private static Object convertToObject(EditPropertyAction editPropertyAction, Field field, EntityType entityType) {
    Object value = editPropertyAction.getValue();
    try {
      if (entityType.getIdNameFields().contains(editPropertyAction.getName())) {
        return objectMapper.readValue(valueOf(value), IdName.class).getId();
      }
      return objectMapper.readValue(valueOf(value), field.getType());
    } catch (JsonProcessingException e) {
      log.error("error while converting object to JSON String. {}", value);
    }
    return value;
  }

  private static Object convertToWrapper(Object value, Field field) {
    Class<?> fieldType = field.getType();
    String stringValue = valueOf(value);
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
    if (fieldType.isAssignableFrom(Date.class)) {
      try {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(stringValue);
      } catch (ParseException e) {
        log.error("{} can not be parsed to date", stringValue);
      }
    }
    return value;
  }

  private static Object getJsonValue(String dbData) {
    try {
      return objectMapper.readTree(dbData);
    } catch (JsonProcessingException e) {
      return isParsable(dbData) ? createNumber(dbData)
          : convertEntityAttributeToBoolean(dbData);
    }
  }

  private static Object convertEntityAttributeToBoolean(String dbData) {
    return dbData.equalsIgnoreCase("true") || dbData.equalsIgnoreCase("false") ? toBooleanObject(dbData) : dbData.replace("'", "");
  }

}