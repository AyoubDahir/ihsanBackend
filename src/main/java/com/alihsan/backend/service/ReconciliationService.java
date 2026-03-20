package com.alihsan.backend.service;

import com.alihsan.backend.domain.InvoicePaymentIntent;
import com.alihsan.backend.domain.InvoicePaymentIntentStatus;
import com.alihsan.backend.domain.PaymentIntent;
import com.alihsan.backend.domain.PaymentIntentStatus;
import com.alihsan.backend.repository.InvoicePaymentIntentRepository;
import com.alihsan.backend.repository.PaymentIntentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Reconciles PENDING payment intents that never received a Waafi webhook callback.
 * Runs every 5 minutes, picks up intents older than 3 minutes, polls Waafi for their
 * actual status, and drives the same logic as the webhook handler.
 */
@Service
public class ReconciliationService {
    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);
    private static final int PENDING_AGE_MINUTES = 3;

    private final PaymentIntentRepository paymentIntentRepository;
    private final InvoicePaymentIntentRepository invoicePaymentIntentRepository;
    private final PaymentProviderService paymentProviderService;
    private final PaymentIntentService paymentIntentService;
    private final InvoicePaymentIntentService invoicePaymentIntentService;

    public ReconciliationService(
        PaymentIntentRepository paymentIntentRepository,
        InvoicePaymentIntentRepository invoicePaymentIntentRepository,
        PaymentProviderService paymentProviderService,
        PaymentIntentService paymentIntentService,
        InvoicePaymentIntentService invoicePaymentIntentService
    ) {
        this.paymentIntentRepository = paymentIntentRepository;
        this.invoicePaymentIntentRepository = invoicePaymentIntentRepository;
        this.paymentProviderService = paymentProviderService;
        this.paymentIntentService = paymentIntentService;
        this.invoicePaymentIntentService = invoicePaymentIntentService;
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void reconcileAppointmentPayments() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(PENDING_AGE_MINUTES);
        List<PaymentIntent> stale = paymentIntentRepository.findByStatusAndCreatedAtBefore(
            PaymentIntentStatus.PENDING, cutoff
        );
        if (stale.isEmpty()) return;

        log.info("Reconciliation: found {} stale PENDING appointment payment intents", stale.size());
        for (PaymentIntent intent : stale) {
            try {
                Map<String, Object> result = paymentProviderService.checkTransactionStatus(intent.getReferenceId());
                String waafiStatus = stringVal(result.get("status"));
                String providerTxnId = stringVal(result.get("transactionId"));
                log.info("Reconciliation APPT {}: Waafi status={}", intent.getReferenceId(), waafiStatus);

                if ("APPROVED".equalsIgnoreCase(waafiStatus)) {
                    paymentIntentService.handleWebhook(intent.getReferenceId(), "APPROVED");
                    log.info("Reconciliation APPT {}: processed as APPROVED", intent.getReferenceId());
                } else if (isTerminalFailure(waafiStatus)) {
                    paymentIntentService.handleWebhook(intent.getReferenceId(), "FAILED");
                    log.info("Reconciliation APPT {}: processed as FAILED ({})", intent.getReferenceId(), waafiStatus);
                }
                // If still PENDING (user hasn't entered PIN yet), leave it for next run
            } catch (Exception ex) {
                log.error("Reconciliation APPT {}: error - {}", intent.getReferenceId(), ex.getMessage());
            }
        }
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void reconcileInvoicePayments() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(PENDING_AGE_MINUTES);
        List<InvoicePaymentIntent> stale = invoicePaymentIntentRepository.findByStatusAndCreatedAtBefore(
            InvoicePaymentIntentStatus.PENDING, cutoff
        );
        if (stale.isEmpty()) return;

        log.info("Reconciliation: found {} stale PENDING invoice payment intents", stale.size());
        for (InvoicePaymentIntent intent : stale) {
            try {
                Map<String, Object> result = paymentProviderService.checkTransactionStatus(intent.getReferenceId());
                String waafiStatus = stringVal(result.get("status"));
                String providerTxnId = stringVal(result.get("transactionId"));
                log.info("Reconciliation INV {}: Waafi status={}", intent.getReferenceId(), waafiStatus);

                if ("APPROVED".equalsIgnoreCase(waafiStatus)) {
                    invoicePaymentIntentService.handleWebhook(intent.getReferenceId(), "APPROVED", providerTxnId);
                    log.info("Reconciliation INV {}: processed as APPROVED", intent.getReferenceId());
                } else if (isTerminalFailure(waafiStatus)) {
                    invoicePaymentIntentService.handleWebhook(intent.getReferenceId(), "FAILED", null);
                    log.info("Reconciliation INV {}: processed as FAILED ({})", intent.getReferenceId(), waafiStatus);
                }
            } catch (Exception ex) {
                log.error("Reconciliation INV {}: error - {}", intent.getReferenceId(), ex.getMessage());
            }
        }
    }

    private boolean isTerminalFailure(String status) {
        if (status == null) return false;
        String s = status.toUpperCase();
        return s.equals("DECLINED") || s.equals("FAILED") || s.equals("CANCELLED") || s.equals("EXPIRED");
    }

    private String stringVal(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
