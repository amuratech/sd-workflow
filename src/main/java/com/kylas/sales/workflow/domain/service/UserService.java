package com.kylas.sales.workflow.domain.service;

import com.kylas.sales.workflow.domain.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class UserService {

  private final String clientBasePath;

  @Autowired
  public UserService(@Value("${client.iam.basePath}") String clientBasePath) {
    this.clientBasePath = clientBasePath;
  }

  public Mono<User> getUserDetails(long userId, String authenticationToken) {
    return WebClient.builder()
        .baseUrl(clientBasePath)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authenticationToken)
        .build()
        .get()
        .uri(uriBuilder -> uriBuilder.path("/v1/users").path("/" + userId).build())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(User.class);
  }
}
