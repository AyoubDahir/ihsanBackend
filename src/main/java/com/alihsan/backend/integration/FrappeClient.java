package com.alihsan.backend.integration;

import com.alihsan.backend.config.PrimeProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FrappeClient {
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String token;
    private final String host;

    public FrappeClient(PrimeProperties primeProperties) {
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = primeProperties.baseUrl();
        this.token = "token " + primeProperties.apiKey() + ":" + primeProperties.apiSecret();
        this.host = primeProperties.host() != null ? primeProperties.host() : "alihsans.com";
    }

    public Map<String, Object> postMethod(String methodPath, Map<String, Object> payload) {
        try {
            String url = baseUrl + "/api/method/" + methodPath;
            String jsonBody = objectMapper.writeValueAsString(payload);
            
            HttpPost request = new HttpPost(url);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Authorization", token);
            request.setHeader("Host", host);
            request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
            
            return httpClient.execute(request, response -> {
                String body = EntityUtils.toString(response.getEntity());
                return objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Prime API: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getResource(String resourcePath, Map<String, Object> queryParams) {
        try {
            String queryString = queryParams.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
            String url = baseUrl + "/api/resource/" + resourcePath + (queryString.isEmpty() ? "" : "?" + queryString);
            
            HttpGet request = new HttpGet(url);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Authorization", token);
            request.setHeader("Host", host);
            
            return httpClient.execute(request, response -> {
                String body = EntityUtils.toString(response.getEntity());
                return objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Prime API: " + e.getMessage(), e);
        }
    }
}
