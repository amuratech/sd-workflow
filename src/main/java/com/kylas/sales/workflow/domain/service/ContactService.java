package com.kylas.sales.workflow.domain.service;

import com.kylas.sales.workflow.domain.processor.contact.ContactResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ContactService {

  private final String clientBasePath;

  @Autowired
  public ContactService(@Value("${client.sales.basePath}") String clientBasePath) {
    this.clientBasePath = clientBasePath;
  }

  public ContactResponse getContactById(long contactId, String authenticationToken) {
    return WebClient.builder()
        .baseUrl(clientBasePath)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authenticationToken)
        .build()
        .get()
        .uri(uriBuilder -> uriBuilder.path("/v1/contacts").queryParam("contactId", contactId).build())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(ContactResponse.class)
        .block();
  }

}
