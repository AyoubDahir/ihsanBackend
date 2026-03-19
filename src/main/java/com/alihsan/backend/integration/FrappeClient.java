package com.alihsan.backend.integration;

import com.alihsan.backend.config.PrimeProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FrappeClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String token;
    private final String host;

    public FrappeClient(PrimeProperties primeProperties) {
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = primeProperties.baseUrl();
        this.token = "token " + primeProperties.apiKey() + ":" + primeProperties.apiSecret();
        this.host = primeProperties.host() != null ? primeProperties.host() : "alihsans.com";
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> postMethod(String methodPath, Map<String, Object> payload) {
        try {
            String url = baseUrl + "/api/method/" + methodPath;
            String jsonBody = objectMapper.writeValueAsString(payload);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", token)
                .header("Host", host)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Prime API: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getResource(String resourcePath, Map<String, Object> queryParams) {
        try {
            String queryString = queryParams.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
            String url = baseUrl + "/api/resource/" + resourcePath + (queryString.isEmpty() ? "" : "?" + queryString);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", token)
                .header("Host", host)
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Prime API: " + e.getMessage(), e);
        }
    }
}
