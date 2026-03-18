package com.alihsan.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "invoice_payment_intents")
public class InvoicePaymentIntent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_id", nullable = false, unique = true, length = 80)
    private String referenceId;

    @Column(name = "patient_id", nullable = false, length = 140)
    private String patientId;

    @Column(name = "invoice_id", nullable = false, length = 140)
    private String invoiceId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "payment_type", nullable = false, length = 30)
    private String paymentType;

    @Column(name = "provider_txn_id", length = 140)
    private String providerTxnId;

    @Column(name = "prime_payment_entry", length = 140)
    private String primePaymentEntry;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private InvoicePaymentIntentStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getProviderTxnId() {
        return providerTxnId;
    }

    public void setProviderTxnId(String providerTxnId) {
        this.providerTxnId = providerTxnId;
    }

    public String getPrimePaymentEntry() {
        return primePaymentEntry;
    }

    public void setPrimePaymentEntry(String primePaymentEntry) {
        this.primePaymentEntry = primePaymentEntry;
    }

    public InvoicePaymentIntentStatus getStatus() {
        return status;
    }

    public void setStatus(InvoicePaymentIntentStatus status) {
        this.status = status;
    }
}
