package com.kylas.sales.workflow.domain.service;

import com.kylas.sales.workflow.domain.entity.EntityDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Service
public class ConfigService {

  private final String clientBasePath;

  @Autowired
  public ConfigService(@Value("${client.config.basePath}") String clientBasePath) {
    this.clientBasePath = clientBasePath;
  }

  public Flux<EntityDefinition> getFields(String entity, String authenticationToken) {
    return WebClient.builder()
        .baseUrl(clientBasePath)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authenticationToken)
        .build()
        .get()
        .uri("/v1/entities/{entity}/fields", entity.toLowerCase())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToFlux(EntityDefinition.class);
  }
}
