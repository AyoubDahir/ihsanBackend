package com.alihsan.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hormuud.sms")
public record HormuudSmsProperties(
    String tokenUrl,
    String sendUrl,
    String username,
    String password,
    String senderId
) {
}
