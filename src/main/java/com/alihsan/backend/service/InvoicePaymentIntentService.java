package com.alihsan.backend.service;

import com.alihsan.backend.domain.InvoicePaymentIntent;
import com.alihsan.backend.domain.InvoicePaymentIntentStatus;
import com.alihsan.backend.dto.BillingInvoiceView;
import com.alihsan.backend.dto.CreateInvoicePaymentRequest;
import com.alihsan.backend.repository.InvoicePaymentIntentRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class InvoicePaymentIntentService {
    private static final Logger log = LoggerFactory.getLogger(InvoicePaymentIntentService.class);
    private final InvoicePaymentIntentRepository invoicePaymentIntentRepository;
    private final PaymentProviderService paymentProviderService;
    private final PrimeWorkflowService primeWorkflowService;

    public InvoicePaymentIntentService(
        InvoicePaymentIntentRepository invoicePaymentIntentRepository,
        PaymentProviderService paymentProviderService,
        PrimeWorkflowService primeWorkflowService
    ) {
        this.invoicePaymentIntentRepository = invoicePaymentIntentRepository;
        this.paymentProviderService = paymentProviderService;
        this.primeWorkflowService = primeWorkflowService;
    }

    @Transactional
    public InvoicePaymentIntent createIntent(CreateInvoicePaymentRequest request) {
        BillingInvoiceView invoice = primeWorkflowService.findUnpaidInvoice(request.patientId(), request.invoiceId());
        if (invoice == null) {
            throw new IllegalArgumentException("Unpaid invoice not found for this patient: " + request.invoiceId());
        }

        BigDecimal outstanding = parseAmount(invoice.outstandingAmount(), "outstandingAmount");
        BigDecimal amount = request.amount() == null ? outstanding : request.amount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        if (amount.compareTo(outstanding) > 0) {
            throw new IllegalArgumentException("Payment amount cannot exceed outstanding invoice amount");
        }

        String referenceId = "INV-" + UUID.randomUUID();
        InvoicePaymentIntent intent = new InvoicePaymentIntent();
        intent.setReferenceId(referenceId);
        intent.setPatientId(request.patientId());
        intent.setInvoiceId(request.invoiceId());
        intent.setAmount(amount);
        intent.setPhoneNumber(request.phoneNumber());
        intent.setPaymentType(request.paymentType());
        intent.setStatus(InvoicePaymentIntentStatus.PENDING);

        invoicePaymentIntentRepository.save(intent);
        try {
            paymentProviderService.initiatePayment(referenceId, amount, request.phoneNumber(), request.paymentType());
        } catch (Exception ex) {
            intent.setStatus(InvoicePaymentIntentStatus.FAILED);
            invoicePaymentIntentRepository.save(intent);
            throw ex;
        }

        // Waafi is synchronous — reaching here means payment is APPROVED.
        // Immediately mark the invoice as paid in Frappe.
        intent.setStatus(InvoicePaymentIntentStatus.APPROVED);
        invoicePaymentIntentRepository.save(intent);
        try {
            Map<String, Object> response = primeWorkflowService.markSalesInvoicePaidFromMobile(
                intent.getInvoiceId(),
                intent.getAmount().toPlainString(),
                intent.getPaymentType(),
                intent.getReferenceId(),
                null
            );
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) response.getOrDefault("message", Map.of());
            intent.setPrimePaymentEntry(asString(message.get("payment_entry")));
            intent.setStatus(InvoicePaymentIntentStatus.PAID);
            log.info("Invoice {} paid in Frappe for {}", intent.getInvoiceId(), referenceId);
        } catch (Exception ex) {
            log.error("Frappe invoice payment failed after Waafi approval for {}: {}", referenceId, ex.getMessage());
        }
        return invoicePaymentIntentRepository.save(intent);
    }

    @Transactional
    public InvoicePaymentIntent handleWebhook(String referenceId, String providerStatus, String providerTxnId) {
        InvoicePaymentIntent intent = invoicePaymentIntentRepository.findByReferenceId(referenceId)
            .orElseThrow(() -> new IllegalArgumentException("Invoice payment intent not found: " + referenceId));

        if (intent.getStatus() == InvoicePaymentIntentStatus.PAID) {
            return intent;
        }

        if ("APPROVED".equalsIgnoreCase(providerStatus)) {
            intent.setStatus(InvoicePaymentIntentStatus.APPROVED);
            intent.setProviderTxnId(providerTxnId);
            Map<String, Object> response = primeWorkflowService.markSalesInvoicePaidFromMobile(
                intent.getInvoiceId(),
                intent.getAmount().toPlainString(),
                intent.getPaymentType(),
                intent.getReferenceId(),
                providerTxnId
            );
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) response.getOrDefault("message", Map.of());
            intent.setPrimePaymentEntry(asString(message.get("payment_entry")));
            intent.setStatus(InvoicePaymentIntentStatus.PAID);
        } else {
            intent.setStatus(InvoicePaymentIntentStatus.FAILED);
        }
        return invoicePaymentIntentRepository.save(intent);
    }

    public Optional<InvoicePaymentIntent> findByReference(String referenceId) {
        return invoicePaymentIntentRepository.findByReferenceId(referenceId);
    }

    private String asString(Object val) {
        return val == null ? null : String.valueOf(val);
    }

    private BigDecimal parseAmount(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Invoice " + fieldName + " is missing");
        }
        try {
            return new BigDecimal(value.trim());
        } catch (Exception ex) {
            throw new IllegalStateException("Invoice " + fieldName + " is invalid: " + value);
        }
    }
}
