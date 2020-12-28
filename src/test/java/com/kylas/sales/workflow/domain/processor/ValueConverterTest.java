package com.kylas.sales.workflow.domain.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kylas.sales.workflow.domain.processor.lead.Email;
import com.kylas.sales.workflow.domain.processor.lead.EmailType;
import com.kylas.sales.workflow.domain.processor.lead.PhoneNumber;
import com.kylas.sales.workflow.domain.processor.lead.PhoneType;
import com.kylas.sales.workflow.domain.processor.lead.Product;
import com.kylas.sales.workflow.domain.workflow.action.ValueConverter;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
public class ValueConverterTest {

  @InjectMocks
  private ValueConverter valueConverter;
  @InjectMocks
  private ObjectMapper objectMapper;

  @Test
  public void givenString_shouldConvertToDatabaseColumn_andReturnString() {
    //given
    String value = "Bhaskar";
    //when
    String actual = valueConverter.convertToDatabaseColumn(value);
    //then
    assertThat(actual).isEqualTo("'Bhaskar'");
  }

  @Test
  public void givenInteger_shouldConvertToDatabaseColumn_andReturnString() {
    //given
    Number value = 123;
    //when
    String actual = valueConverter.convertToDatabaseColumn(value);
    //then
    assertThat(actual).isEqualTo(String.valueOf(value));
  }

  @Test
  public void givenDouble_shouldConvertToDatabaseColumn_andReturnString() {
    //given
    Number value = 123.2445;
    //when
    String actual = valueConverter.convertToDatabaseColumn(value);
    //then
    assertThat(actual).isEqualTo(String.valueOf(value));
  }

  @Test
  public void givenBoolean_shouldConvertToDatabaseColumn_andReturnString() {
    //given
    Boolean value = Boolean.TRUE;
    //when
    String actual = valueConverter.convertToDatabaseColumn(value);
    //then
    assertThat(actual).isEqualTo(String.valueOf(value));
  }

  @Test
  public void givenJsonObject_shouldConvertToDatabaseColumn_andReturnString() throws JsonProcessingException {
    //given
    Product value = new Product(1, "cellphone");
    //when
    String actual = valueConverter.convertToDatabaseColumn(value);
    //then
    assertThat(actual).isEqualTo(objectMapper.writeValueAsString(value));
  }

  @Test
  public void givenJsonArray_shouldConvertToDatabaseColumn_andReturnString() throws JsonProcessingException {
    //given
    List<Product> products = List.of(new Product(1, "cellphone"), new Product(1, "telephone"));
    PhoneNumber[] phoneNumbers = {
        new PhoneNumber(PhoneType.MOBILE, "IN", "9999999999", "+91", true),
        new PhoneNumber(PhoneType.HOME, "IN", "9999999999", "+91", true)
    };
    Email[] emails = {new Email(EmailType.OFFICE, "abc@gmail.com", true), new Email(EmailType.PERSONAL, "abc@yahoo.in", true)};
    //when
    String actualProducts = valueConverter.convertToDatabaseColumn(products);
    String actualPhoneNumbers = valueConverter.convertToDatabaseColumn(phoneNumbers);
    String actualEmails = valueConverter.convertToDatabaseColumn(emails);
    //then
    assertThat(actualProducts).isEqualTo(objectMapper.writeValueAsString(products));
    assertThat(actualPhoneNumbers).isEqualTo(objectMapper.writeValueAsString(phoneNumbers));
    assertThat(actualEmails).isEqualTo(objectMapper.writeValueAsString(emails));
  }

  @Test
  public void givenValueAsString_shouldConvertToEntityAttribute_andReturnString() {
    //given
    String value = "Bhaskar";
    //when
    Object actual = valueConverter.convertToEntityAttribute(value);
    //then
    assertThat(actual).isEqualTo(value);
  }

  @Test
  public void givenIntegerAsString_shouldConvertToEntityAttribute_andReturnInteger() {
    //given
    String value = "1234";
    //when
    Object actual = valueConverter.convertToEntityAttribute(value);
    //then
    assertThat(actual).isEqualTo(IntNode.valueOf(1234));
  }

  @Test
  public void givenDoubleAsString_shouldConvertToEntityAttribute_andReturnDouble() {
    //given
    String value = "123.2445";
    //when
    Object actual = valueConverter.convertToEntityAttribute(value);
    //then
    assertThat(actual).isEqualTo(DoubleNode.valueOf(123.2445));
  }

  @Test
  public void givenBooleanAsString_shouldConvertToEntityAttribute_andReturnBoolean() {
    //given
    String value = "true";
    //when
    Object actual = valueConverter.convertToEntityAttribute(value);
    //then
    assertThat(actual).isEqualTo(BooleanNode.valueOf(true));
  }

  @Test
  public void givenJsonObjectAsString_shouldConvertToEntityAttribute_andReturnObjectNode() throws IOException {
    //given
    String value = "{\"id\":1,\"name\":\"cellphone\"}";
    //when
    Object actual = valueConverter.convertToEntityAttribute(value);
    //then
    assertThat(actual).isInstanceOf(ObjectNode.class);
    assertThat(actual).hasToString(value);
  }

  @Test
  public void givenJsonArraysAsString_shouldConvertToEntityAttribute_andReturnArrayNode() throws IOException {
    //given
    String products = "[{\"id\":1,\"name\":\"cellphone\"}]";
    String phoneNumbers = "[{\"type\":\"MOBILE\",\"code\":\"IN\",\"dialCode\":\"+91\",\"value\":\"9999999999\",\"primary\":true}]";
    String emails = "[{\"type\":\"PERSONAL\",\"primary\":true,\"value\":\"bhaskar26.pawar@gmail.com\"}]";
    //when
    Object actualProducts = valueConverter.convertToEntityAttribute(products);
    Object actualPhoneNumbers = valueConverter.convertToEntityAttribute(phoneNumbers);
    Object actualEmails = valueConverter.convertToEntityAttribute(emails);
    //then
    assertThat(actualProducts).hasToString(products);
    assertThat(actualPhoneNumbers).hasToString(phoneNumbers);
    assertThat(actualEmails).hasToString(emails);
  }
}
