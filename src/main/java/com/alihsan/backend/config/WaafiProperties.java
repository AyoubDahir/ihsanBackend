package com.alihsan.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "waafi")
public record WaafiProperties(
    String url,
    String merchantUid,
    String apiUserId,
    String apiKey,
    String channelName
) {}
