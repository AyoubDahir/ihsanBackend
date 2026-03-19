package com.alihsan.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "prime")
public record PrimeProperties(
    String baseUrl,
    String apiKey,
    String apiSecret,
    String host
) {}
