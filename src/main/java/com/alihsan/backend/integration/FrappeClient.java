package com.alihsan.backend.integration;

import com.alihsan.backend.config.PrimeProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
public class FrappeClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String token;
    private final String host;

    public FrappeClient(PrimeProperties primeProperties) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = primeProperties.baseUrl();
        this.token = "token " + primeProperties.apiKey() + ":" + primeProperties.apiSecret();
        this.host = primeProperties.host() != null ? primeProperties.host() : "alihsans.com";
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, token);
        headers.set(HttpHeaders.HOST, host);
        return headers;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> postMethod(String methodPath, Map<String, Object> payload) {
        String url = baseUrl + "/api/method/" + methodPath;
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, createHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getResource(String resourcePath, Map<String, Object> queryParams) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/api/resource/" + resourcePath);
        queryParams.forEach((k, v) -> builder.queryParam(k, v));
        
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, entity, Map.class);
        return response.getBody();
    }
}
