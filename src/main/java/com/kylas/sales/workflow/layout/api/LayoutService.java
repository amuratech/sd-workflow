package com.kylas.sales.workflow.layout.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylas.sales.workflow.layout.api.exception.LayoutNotFoundException;
import com.kylas.sales.workflow.layout.api.response.list.ListLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LayoutService {

  private final ObjectMapper objectMapper;

  @Autowired
  public LayoutService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public ListLayout getListLayout() {
    try {
      var createLayoutJson = readFromFile("/layout/list.json");
      return objectMapper.readValue(createLayoutJson, ListLayout.class);
    } catch (IOException e) {
      log.error("Error occurred while trying to locate list view: ");
      throw new LayoutNotFoundException();
    }
  }

  private static String readFromFile(String resourcePath) throws IOException {
    Resource resource = new ClassPathResource(resourcePath);
    InputStream inputStream;

    inputStream = resource.getInputStream();
    return readFromInputStream(inputStream);
  }

  private static String readFromInputStream(InputStream inputStream) throws IOException {
    StringBuilder resultStringBuilder = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
      String line;
      while ((line = br.readLine()) != null) {
        resultStringBuilder.append(line).append("\n");
      }
    }
    return resultStringBuilder.toString();
  }
}
