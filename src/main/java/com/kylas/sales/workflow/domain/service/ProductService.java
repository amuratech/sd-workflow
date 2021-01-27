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
public class ProductService {
    private final String clientBasePath;

    @Autowired
    public ProductService(@Value("${client.product.basePath}") String clientBasePath) {
        this.clientBasePath = clientBasePath;
    }

    public Mono<IdName> getProduct(Long productId, String authenticationToken) {
        return WebClient.builder()
                .baseUrl(clientBasePath)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authenticationToken)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder.path("/v1/products/" + productId).build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(IdName.class);
    }
}
