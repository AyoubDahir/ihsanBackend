package com.alihsan.backend.service;

import com.alihsan.backend.config.HormuudSmsProperties;
import com.alihsan.backend.util.MobileNumberUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class SmsService {
    private final HormuudSmsProperties smsProperties;
    private final WebClient webClient;

    public SmsService(HormuudSmsProperties smsProperties) {
        this.smsProperties = smsProperties;
        this.webClient = WebClient.builder().build();
    }

    public void sendCalledSms(String mobile, String patientName) {
        validateConfig();
        String normalized = MobileNumberUtil.normalize(mobile);
        if (normalized == null || normalized.isBlank()) return;

        String localMobile = normalized.startsWith("252") ? normalized.substring(3) : normalized;
        String token = getAccessToken();
        String message = "Salam " + patientName + ",\n\n" +
            "Waqtigii ballantaadu waa la gaadhay. Fadlan si degdeg ah ugu gudub dhakhtarka.\n\n" +
            "Fadlan ogow: haddii aadan waqtigan ku iman, waxaa la siin doonaa fursadda bukaanka kugu xiga.\n\n" +
            "Mahadsanid,\nAl-Ihsan Hospital";

        Map<String, Object> payload = Map.of(
            "senderid", smsProperties.senderId(),
            "mobile", localMobile,
            "message", message
        );

        webClient.post()
            .uri(smsProperties.sendUrl())
            .contentType(MediaType.APPLICATION_JSON)
            .headers(h -> h.setBearerAuth(token))
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }

    public void sendOtp(String mobile, String otp) {
        validateConfig();
        String normalized = MobileNumberUtil.normalize(mobile);
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException("mobile is required for OTP SMS");
        }

        String localMobile = normalized.startsWith("252") ? normalized.substring(3) : normalized;
        String token = getAccessToken();
        String message = "Your Alihsan Hospital login code is: " + otp;

        Map<String, Object> payload = Map.of(
            "senderid", smsProperties.senderId(),
            "mobile", localMobile,
            "message", message
        );

        webClient.post()
            .uri(smsProperties.sendUrl())
            .contentType(MediaType.APPLICATION_JSON)
            .headers(h -> h.setBearerAuth(token))
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }

    @SuppressWarnings("unchecked")
    private String getAccessToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("username", smsProperties.username());
        form.add("password", smsProperties.password());

        Map<String, Object> tokenResponse = webClient.post()
            .uri(smsProperties.tokenUrl())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue(form)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        if (tokenResponse == null) {
            throw new IllegalStateException("Failed to get SMS token response");
        }

        Object accessToken = tokenResponse.get("access_token");
        if (accessToken == null || String.valueOf(accessToken).isBlank()) {
            throw new IllegalStateException("SMS provider did not return access token");
        }
        return String.valueOf(accessToken);
    }

    private void validateConfig() {
        if (isBlank(smsProperties.tokenUrl())
            || isBlank(smsProperties.sendUrl())
            || isBlank(smsProperties.username())
            || isBlank(smsProperties.password())
            || isBlank(smsProperties.senderId())) {
            throw new IllegalStateException("Hormuud SMS credentials/config are incomplete");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
