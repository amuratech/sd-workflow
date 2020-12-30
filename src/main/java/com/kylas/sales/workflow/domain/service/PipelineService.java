package com.kylas.sales.workflow.domain.service;

import com.kylas.sales.workflow.domain.processor.lead.IdName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class PipelineService {

  private final String clientBasePath;

  @Autowired
  public PipelineService(@Value("${client.sales.basePath}") String clientBasePath) {
    this.clientBasePath = clientBasePath;
  }

  public Mono<IdName> getPipeline(Long pipelineId, String authenticationToken) {
    return WebClient.builder()
        .baseUrl(clientBasePath)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authenticationToken)
        .build()
        .get()
        .uri(uriBuilder -> uriBuilder.path("/v1/pipelines/" + pipelineId).build())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(IdName.class);
  }
}
