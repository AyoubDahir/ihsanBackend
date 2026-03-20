package com.alihsan.backend.controller;

import com.alihsan.backend.dto.PaymentWebhookRequest;
import com.alihsan.backend.service.InvoicePaymentIntentService;
import com.alihsan.backend.service.PaymentIntentService;
import com.alihsan.backend.service.PaymentProviderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    private final PaymentIntentService paymentIntentService;
    private final InvoicePaymentIntentService invoicePaymentIntentService;
    private final PaymentProviderService paymentProviderService;

    public PaymentController(
        PaymentIntentService paymentIntentService,
        InvoicePaymentIntentService invoicePaymentIntentService,
        PaymentProviderService paymentProviderService
    ) {
        this.paymentIntentService = paymentIntentService;
        this.invoicePaymentIntentService = invoicePaymentIntentService;
        this.paymentProviderService = paymentProviderService;
    }

    @PostMapping("/webhook")
    public Map<String, Object> paymentWebhook(@Valid @RequestBody PaymentWebhookRequest request) {
        if (paymentIntentService.findByReference(request.referenceId()).isPresent()) {
            var intent = paymentIntentService.handleWebhook(request.referenceId(), request.status());
            Map<String, Object> out = new HashMap<>();
            out.put("referenceId", intent.getReferenceId());
            out.put("type", "APPOINTMENT");
            out.put("status", intent.getStatus().name());
            out.put("primeQue", intent.getPrimeQue());
            out.put("primeInvoice", intent.getPrimeInvoice());
            out.put("patientName", intent.getPatientName());
            out.put("practitionerName", intent.getPractitionerName());
            out.put("primePaymentEntry", intent.getPrimePaymentEntry());
            return out;
        }
        if (invoicePaymentIntentService.findByReference(request.referenceId()).isPresent()) {
            var intent = invoicePaymentIntentService.handleWebhook(
                request.referenceId(),
                request.status(),
                request.providerTxnId()
            );
            Map<String, Object> out = new HashMap<>();
            out.put("referenceId", intent.getReferenceId());
            out.put("type", "INVOICE");
            out.put("status", intent.getStatus().name());
            out.put("invoiceId", intent.getInvoiceId());
            out.put("paymentEntry", intent.getPrimePaymentEntry());
            return out;
        }
        throw new IllegalArgumentException("Payment reference not found: " + request.referenceId());
    }

    @GetMapping("/verify/{referenceId}")
    public Map<String, Object> verifyPayment(@PathVariable String referenceId) {
        return paymentProviderService.checkTransactionStatus(referenceId);
    }

    /**
     * Returns the booking pass details for a confirmed appointment.
     * The mobile app shows a QR code encoding the referenceId; the hospital
     * cashier scans it here to instantly look up the patient's queue position.
     */
    @GetMapping("/pass/{referenceId}")
    public Map<String, Object> getBookingPass(@PathVariable String referenceId) {
        var intent = paymentIntentService.findByReference(referenceId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + referenceId));

        Map<String, Object> out = new HashMap<>();
        out.put("referenceId", intent.getReferenceId());
        out.put("status", intent.getStatus().name());
        out.put("patientName", intent.getPatientName());
        out.put("practitionerName", intent.getPractitionerName());
        out.put("appointmentDate", intent.getAppointmentDate());
        out.put("appointmentTime", intent.getAppointmentTime());
        out.put("department", intent.getDepartment());
        out.put("amount", intent.getAmount());
        out.put("primeQue", intent.getPrimeQue());
        out.put("primeInvoice", intent.getPrimeInvoice());
        out.put("confirmed", "APPOINTMENT_CREATED".equals(intent.getStatus().name()));
        return out;
    }
}
