package com.alihsan.backend.controller;

import com.alihsan.backend.dto.BillingInvoiceView;
import com.alihsan.backend.dto.CreateInvoicePaymentRequest;
import com.alihsan.backend.dto.CreateInvoicePaymentResponse;
import com.alihsan.backend.service.InvoicePaymentIntentService;
import com.alihsan.backend.service.PrimeWorkflowService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/billing")
public class BillingController {
    private final PrimeWorkflowService primeWorkflowService;
    private final InvoicePaymentIntentService invoicePaymentIntentService;

    public BillingController(
        PrimeWorkflowService primeWorkflowService,
        InvoicePaymentIntentService invoicePaymentIntentService
    ) {
        this.primeWorkflowService = primeWorkflowService;
        this.invoicePaymentIntentService = invoicePaymentIntentService;
    }

    @GetMapping("/invoices")
    public List<BillingInvoiceView> getUnpaidInvoices(@RequestParam("patientId") String patientId) {
        return primeWorkflowService.getUnpaidInvoices(patientId);
    }

    @GetMapping("/invoices/paid")
    public List<BillingInvoiceView> getPaidInvoices(@RequestParam("patientId") String patientId) {
        return primeWorkflowService.getPaidInvoices(patientId);
    }

    @GetMapping("/invoices/{invoiceId}")
    public BillingInvoiceView getInvoice(
        @PathVariable String invoiceId,
        @RequestParam("patientId") String patientId
    ) {
        BillingInvoiceView invoice = primeWorkflowService.findUnpaidInvoice(patientId, invoiceId);
        if (invoice == null) {
            throw new IllegalArgumentException("Unpaid invoice not found: " + invoiceId);
        }
        return invoice;
    }

    @PostMapping("/pay")
    public CreateInvoicePaymentResponse createInvoicePayment(@Valid @RequestBody CreateInvoicePaymentRequest request) {
        var intent = invoicePaymentIntentService.createIntent(request);
        return new CreateInvoicePaymentResponse(
            intent.getReferenceId(),
            intent.getInvoiceId(),
            intent.getStatus().name(),
            "Invoice payment initiated. Wait for webhook confirmation."
        );
    }

    @GetMapping("/payments/{referenceId}")
    public Map<String, Object> getPayment(@PathVariable String referenceId) {
        var intent = invoicePaymentIntentService.findByReference(referenceId)
            .orElseThrow(() -> new IllegalArgumentException("Invoice payment intent not found: " + referenceId));
        Map<String, Object> out = new HashMap<>();
        out.put("referenceId", intent.getReferenceId());
        out.put("invoiceId", intent.getInvoiceId());
        out.put("status", intent.getStatus().name());
        out.put("paymentEntry", intent.getPrimePaymentEntry());
        out.put("providerTxnId", intent.getProviderTxnId());
        return out;
    }
}
