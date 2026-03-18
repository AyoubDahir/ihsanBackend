package com.alihsan.backend.integration;

import com.alihsan.backend.config.PrimeProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class FrappeClient {
    private final WebClient webClient;

    public FrappeClient(PrimeProperties primeProperties) {
        String token = "token " + primeProperties.apiKey() + ":" + primeProperties.apiSecret();
        this.webClient = WebClient.builder()
            .baseUrl(primeProperties.baseUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, token)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> postMethod(String methodPath, Map<String, Object> payload) {
        return webClient.post()
            .uri("/api/method/" + methodPath)
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(Map.class)
            .block();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getResource(String resourcePath, Map<String, Object> queryParams) {
        return webClient.get()
            .uri(uriBuilder -> {
                var builder = uriBuilder.path("/api/resource/" + resourcePath);
                queryParams.forEach((k, v) -> builder.queryParam(k, v));
                return builder.build();
            })
            .retrieve()
            .bodyToMono(Map.class)
            .block();
    }
}
