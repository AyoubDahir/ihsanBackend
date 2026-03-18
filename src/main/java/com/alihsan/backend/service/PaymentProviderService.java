package com.alihsan.backend.service;

import com.alihsan.backend.config.WaafiProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class PaymentProviderService {
    private static final Pattern NON_DIGIT = Pattern.compile("\\D");
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final WebClient webClient;
    private final WaafiProperties waafiProperties;

    public PaymentProviderService(WaafiProperties waafiProperties) {
        this.waafiProperties = waafiProperties;
        this.webClient = WebClient.builder().baseUrl(waafiProperties.url()).build();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> initiatePayment(String referenceId, BigDecimal amount, String phoneNumber, String paymentType) {
        validateConfig();
        String accountNo = normalizeAccountNo(phoneNumber);
        String amountStr = amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
        String requestId = "REQ-" + System.currentTimeMillis();

        Map<String, Object> payload = Map.of(
            "schemaVersion", "1.0",
            "requestId", requestId,
            "timestamp", waafiTimestamp(),
            "channelName", channel(),
            "serviceName", "API_PURCHASE",
            "serviceParams", Map.of(
                "merchantUid", waafiProperties.merchantUid(),
                "apiUserId", waafiProperties.apiUserId(),
                "apiKey", waafiProperties.apiKey(),
                "paymentMethod", "MWALLET_ACCOUNT",
                "payerInfo", Map.of("accountNo", accountNo),
                "transactionInfo", Map.of(
                    "referenceId", referenceId,
                    "invoiceId", referenceId,
                    "amount", amountStr,
                    "currency", "USD",
                    "description", "Appointment Booking (" + paymentType + ")"
                )
            )
        );

        Map<String, Object> data = webClient.post()
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        String code = stringVal(data.get("responseCode"));
        String msg = stringVal(data.get("responseMsg"));
        if ("2001".equals(code) || "R_SUCCESS".equalsIgnoreCase(msg)) {
            Map<String, Object> params = (Map<String, Object>) data.getOrDefault("params", Map.of());
            Map<String, Object> serviceParams = (Map<String, Object>) data.getOrDefault("serviceParams", Map.of());
            return Map.of(
                "success", true,
                "referenceId", referenceId,
                "requestId", requestId,
                "transactionId", firstNonBlank(
                    stringVal(params.get("transactionId")),
                    stringVal(serviceParams.get("transactionId")),
                    requestId
                ),
                "providerStatus", firstNonBlank(stringVal(params.get("state")), "PENDING"),
                "message", firstNonBlank(msg, "Payment request processed"),
                "raw", data
            );
        }

        throw new IllegalStateException(firstNonBlank(msg, "Waafi payment failed with code: " + code));
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> checkTransactionStatus(String referenceId) {
        validateConfig();
        Map<String, Object> payload = Map.of(
            "schemaVersion", "1.0",
            "requestId", "REQ-GET-" + System.currentTimeMillis(),
            "timestamp", waafiTimestamp(),
            "channelName", channel(),
            "serviceName", "HPP_GETTRANINFO",
            "serviceParams", Map.of(
                "merchantUid", waafiProperties.merchantUid(),
                "apiUserId", waafiProperties.apiUserId(),
                "hppKey", waafiProperties.apiKey(),
                "referenceId", referenceId
            )
        );

        Map<String, Object> data = webClient.post()
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        String code = stringVal(data.get("responseCode"));
        if (!"2001".equals(code)) {
            return Map.of(
                "success", false,
                "message", firstNonBlank(stringVal(data.get("responseMsg")), "Transaction not found"),
                "raw", data
            );
        }

        Map<String, Object> params = (Map<String, Object>) data.getOrDefault("params", Map.of());
        return Map.of(
            "success", true,
            "status", firstNonBlank(stringVal(params.get("tranStatus")), "PENDING"),
            "transactionId", stringVal(params.get("transactionId")),
            "amount", stringVal(params.get("amount")),
            "raw", data
        );
    }

    private void validateConfig() {
        if (isBlank(waafiProperties.url())
            || isBlank(waafiProperties.merchantUid())
            || isBlank(waafiProperties.apiUserId())
            || isBlank(waafiProperties.apiKey())) {
            throw new IllegalStateException("Waafi credentials are not fully configured.");
        }
    }

    private String normalizeAccountNo(String phoneNumber) {
        String accountNo = NON_DIGIT.matcher(firstNonBlank(phoneNumber, "")).replaceAll("");
        if (accountNo.length() != 12 || !accountNo.startsWith("252")) {
            throw new IllegalArgumentException("Phone must be full format like 25261XXXXXXX.");
        }
        return accountNo;
    }

    private String waafiTimestamp() {
        return LocalDateTime.now().format(TS_FMT);
    }

    private String channel() {
        return firstNonBlank(waafiProperties.channelName(), "WEB");
    }

    private String stringVal(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (!isBlank(v)) {
                return v;
            }
        }
        return null;
    }
}
