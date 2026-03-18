package com.alihsan.backend.controller;

import com.alihsan.backend.dto.PaymentWebhookRequest;
import com.alihsan.backend.service.PaymentIntentService;
import com.alihsan.backend.service.PaymentProviderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    private final PaymentIntentService paymentIntentService;
    private final PaymentProviderService paymentProviderService;

    public PaymentController(PaymentIntentService paymentIntentService, PaymentProviderService paymentProviderService) {
        this.paymentIntentService = paymentIntentService;
        this.paymentProviderService = paymentProviderService;
    }

    @PostMapping("/webhook")
    public Map<String, Object> paymentWebhook(@Valid @RequestBody PaymentWebhookRequest request) {
        var intent = paymentIntentService.handleWebhook(request.referenceId(), request.status());
        return Map.of(
            "referenceId", intent.getReferenceId(),
            "status", intent.getStatus().name(),
            "primeQue", intent.getPrimeQue(),
            "primeInvoice", intent.getPrimeInvoice()
        );
    }

    @GetMapping("/verify/{referenceId}")
    public Map<String, Object> verifyPayment(@PathVariable String referenceId) {
        return paymentProviderService.checkTransactionStatus(referenceId);
    }
}
